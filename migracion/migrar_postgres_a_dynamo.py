#!/usr/bin/env python3
"""
Migra los datos de PostgreSQL (alquilerautos_db) a las tablas de DynamoDB
que usa el backend rentacars.

- Conserva los ids originales (los ids siguen siendo numeros).
- Crea las guardas de unicidad en "unicos" (EMAIL#..., PLACA#...).
- Deja cada contador en el id maximo, para que la app siga numerando desde ahi.
- Es idempotente: volver a correrlo sobrescribe los mismos items.

Las tablas deben existir: las crea la app al arrancar, o este script con
--crear-tablas.

Configuracion (variables de entorno; nada de credenciales en el codigo):
  PostgreSQL: PG_HOST (localhost), PG_PORT (5434), PG_DB (alquilerautos_db),
              PG_USER (cloud_usr), PG_PASSWORD (obligatoria)
  DynamoDB:   AWS_REGION (us-east-1), AWS_DYNAMODB_ENDPOINT (vacio = AWS real),
              AWS_DYNAMODB_TABLE_PREFIX (vacio), y las credenciales de AWS
              habituales (AWS_PROFILE o AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY).

Uso:
  pip install -r migracion/requirements.txt
  PG_PASSWORD=... AWS_DYNAMODB_ENDPOINT=http://localhost:8000 \\
  AWS_ACCESS_KEY_ID=local AWS_SECRET_ACCESS_KEY=local \\
      python3 migracion/migrar_postgres_a_dynamo.py [--crear-tablas] [--dry-run]
"""
import argparse
import os
import sys

import boto3
import psycopg2
import psycopg2.extras

PREFIJO = os.environ.get("AWS_DYNAMODB_TABLE_PREFIX", "")


def tabla(nombre):
    return PREFIJO + nombre


# --------------------------------------------------------------------------
# Esquema de las tablas (igual al que crea DynamoDbTableInitializer en la app)
#   nombre: (pk, tipo_pk, [(indice, pk_indice, tipo, sk_indice, tipo_sk), ...])
# --------------------------------------------------------------------------
ESQUEMA = {
    "tiendas": ("id_tienda", "N", [("ciudad-index", "ciudad_lower", "S", None, None)]),
    "categorias": ("id_categoria", "N", [("nombre-index", "nombre", "S", None, None)]),
    "clientes": ("id_cliente", "N", [("email-index", "email", "S", None, None)]),
    "autos": ("id_auto", "N", [
        ("id_tienda-index", "id_tienda", "N", None, None),
        ("id_categoria-index", "id_categoria", "N", None, None),
    ]),
    "detalles_autos": ("id_auto", "N", []),
    "alquileres": ("id_alquiler", "N", [
        ("id_cliente-index", "id_cliente", "N", "fecha_inicio", "S"),
        ("id_auto-index", "id_auto", "N", None, None),
        ("estado-index", "estado", "S", "fecha_fin", "S"),
    ]),
    "contadores": ("entidad", "S", []),
    "unicos": ("valor", "S", []),
}


def crear_tablas(client):
    existentes = set(client.get_paginator("list_tables").paginate().build_full_result()["TableNames"])
    for nombre, (pk, tipo_pk, indices) in ESQUEMA.items():
        if tabla(nombre) in existentes:
            print(f"  tabla {tabla(nombre)} ya existe")
            continue
        atributos = {pk: tipo_pk}
        gsis = []
        for indice, ipk, itipo, isk, isktipo in indices:
            atributos[ipk] = itipo
            esquema = [{"AttributeName": ipk, "KeyType": "HASH"}]
            if isk:
                atributos[isk] = isktipo
                esquema.append({"AttributeName": isk, "KeyType": "RANGE"})
            gsis.append({"IndexName": indice, "KeySchema": esquema,
                         "Projection": {"ProjectionType": "ALL"}})
        params = {
            "TableName": tabla(nombre),
            "BillingMode": "PAY_PER_REQUEST",
            "KeySchema": [{"AttributeName": pk, "KeyType": "HASH"}],
            "AttributeDefinitions": [{"AttributeName": a, "AttributeType": t} for a, t in atributos.items()],
        }
        if gsis:
            params["GlobalSecondaryIndexes"] = gsis
        client.create_table(**params)
        client.get_waiter("table_exists").wait(TableName=tabla(nombre))
        print(f"  tabla {tabla(nombre)} creada")


# --------------------------------------------------------------------------
# Transformacion fila de PostgreSQL -> item de DynamoDB
# (mismos nombres de atributo que las anotaciones @DynamoDbAttribute)
# --------------------------------------------------------------------------
def limpiar(item):
    """DynamoDB no guarda None: el atributo simplemente no se escribe."""
    return {k: v for k, v in item.items() if v is not None}


def iso(fecha):
    return fecha.isoformat() if fecha is not None else None


MIGRACIONES = [
    # (tabla destino, SELECT, transformacion, atributo id para el contador)
    ("tiendas",
     "SELECT id_tienda, nombre, ciudad, direccion FROM tiendas",
     lambda r: {**r, "ciudad_lower": r["ciudad"].lower() if r["ciudad"] else None},
     "id_tienda"),
    ("categorias",
     "SELECT id_categoria, nombre, descripcion FROM categorias",
     lambda r: dict(r),
     "id_categoria"),
    ("clientes",
     "SELECT id_cliente, nombre, email, telefono, tarjeta_credito FROM clientes",
     lambda r: dict(r),
     "id_cliente"),
    ("autos",
     "SELECT id_auto, disponibilidad, id_tienda, id_categoria FROM autos",
     lambda r: dict(r),
     "id_auto"),
    ("detalles_autos",
     "SELECT id_detalles_autos, imagen, modelo, marca, anio, placa, precio_dia, "
     "oferta_porcentaje, id_auto FROM detalles_autos",
     lambda r: dict(r),
     "id_detalles_autos"),
    ("alquileres",
     "SELECT id_alquiler, id_cliente, id_auto, fecha_inicio, fecha_fin, precio_total, "
     "ciudad_retirada, ciudad_devolucion, estado FROM alquileres",
     lambda r: {**r, "fecha_inicio": iso(r["fecha_inicio"]), "fecha_fin": iso(r["fecha_fin"])},
     "id_alquiler"),
]


def guardas_de(nombre, item):
    """Items de la tabla 'unicos' que reemplazan a los UNIQUE de PostgreSQL."""
    if nombre == "clientes" and item.get("email"):
        return [{"valor": f"EMAIL#{item['email']}", "referencia": f"clientes#{item['id_cliente']}"}]
    if nombre == "detalles_autos" and item.get("placa"):
        return [{"valor": f"PLACA#{item['placa']}", "referencia": f"detalles_autos#{item['id_auto']}"}]
    return []


def main():
    parser = argparse.ArgumentParser(description="Migra alquilerautos_db (PostgreSQL) a DynamoDB")
    parser.add_argument("--crear-tablas", action="store_true", help="crea las tablas si no existen")
    parser.add_argument("--dry-run", action="store_true", help="solo lee PostgreSQL y muestra conteos")
    args = parser.parse_args()

    if "PG_PASSWORD" not in os.environ:
        sys.exit("Falta la variable de entorno PG_PASSWORD")

    pg = psycopg2.connect(
        host=os.environ.get("PG_HOST", "localhost"),
        port=int(os.environ.get("PG_PORT", "5434")),
        dbname=os.environ.get("PG_DB", "alquilerautos_db"),
        user=os.environ.get("PG_USER", "cloud_usr"),
        password=os.environ["PG_PASSWORD"],
    )

    endpoint = os.environ.get("AWS_DYNAMODB_ENDPOINT") or None
    region = os.environ.get("AWS_REGION", "us-east-1")
    client = boto3.client("dynamodb", region_name=region, endpoint_url=endpoint)
    dynamo = boto3.resource("dynamodb", region_name=region, endpoint_url=endpoint)
    print(f"DynamoDB: {endpoint or 'AWS ' + region}  prefijo='{PREFIJO}'")

    if args.crear_tablas and not args.dry_run:
        crear_tablas(client)

    unicos = dynamo.Table(tabla("unicos"))
    contadores = dynamo.Table(tabla("contadores"))
    conflictos = 0

    with pg, pg.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
        for nombre, sql, transformar, atributo_id in MIGRACIONES:
            cur.execute(sql)
            items = [limpiar(transformar(fila)) for fila in cur.fetchall()]
            if args.dry_run:
                print(f"  {nombre}: {len(items)} filas")
                continue

            guardas = [g for item in items for g in guardas_de(nombre, item)]
            for guarda in guardas:
                existente = unicos.get_item(Key={"valor": guarda["valor"]}).get("Item")
                if existente and existente.get("referencia") != guarda["referencia"]:
                    print(f"  ! {guarda['valor']} ya pertenece a {existente.get('referencia')}, "
                          f"se omite {guarda['referencia']}")
                    conflictos += 1
            if conflictos:
                sys.exit("Hay valores unicos en conflicto con datos ya existentes en DynamoDB; no se escribio "
                         f"{nombre}. Revisa los mensajes de arriba.")

            with dynamo.Table(tabla(nombre)).batch_writer() as batch:
                for item in items:
                    batch.put_item(Item=item)
            with unicos.batch_writer() as batch:
                for guarda in guardas:
                    batch.put_item(Item=guarda)

            # El contador queda en el id maximo (sin bajarlo si la app ya genero ids mayores)
            maximo = max((int(i[atributo_id]) for i in items), default=0)
            actual = contadores.get_item(Key={"entidad": nombre}).get("Item", {}).get("valor", 0)
            contadores.put_item(Item={"entidad": nombre, "valor": max(maximo, int(actual))})

            print(f"  {nombre}: {len(items)} items, {len(guardas)} guardas, contador={max(maximo, int(actual))}")

    pg.close()
    print("Migracion terminada" if not args.dry_run else "Dry-run terminado (no se escribio nada)")


if __name__ == "__main__":
    main()
