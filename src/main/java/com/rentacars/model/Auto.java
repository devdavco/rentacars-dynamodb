package com.rentacars.model;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

/**
 * Tabla DynamoDB "autos".
 *
 *   PK:  id_auto (N)
 *   GSI: id_tienda-index    -> PK id_tienda (N)     (HU-04 y HU-09 por ciudad)
 *   GSI: id_categoria-index -> PK id_categoria (N)  (HU-09 por categoria)
 *
 * MUY IMPORTANTE -- ESTE ITEM TIENE SOLO 4 ATRIBUTOS.
 * La marca, el modelo, el anio, la placa, el precio y la imagen NO estan aqui:
 * viven en la tabla detalles_autos (clase DetalleAuto).
 *
 * "autos"          -> la unidad fisica y sus relaciones (que tienda, que categoria,
 *                     si esta disponible)
 * "detalles_autos" -> la ficha comercial del vehiculo
 *
 * Por eso HU-08 (registrar auto con detalles) guarda en DOS tablas, y HU-12
 * (ver detalle completo) tiene que leer de las DOS y combinarlas.
 *
 * SOBRE LAS "LLAVES FORANEAS" (idTienda, idCategoria):
 * Se guardan como numeros simples (Long). DynamoDB no valida FKs: lo hacen
 * los services (AutoServiceImpl.createAuto valida que existan; TiendaServiceImpl
 * no deja borrar una tienda con autos).
 */
@DynamoDbBean
@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Auto {

    @Getter(onMethod_ = {@DynamoDbPartitionKey, @DynamoDbAttribute("id_auto")})
    private Long idAuto;

    // HU-08 debe ponerlo en true explicitamente al crear el auto.
    private Boolean disponibilidad;

    @Getter(onMethod_ = {@DynamoDbSecondaryPartitionKey(indexNames = "id_tienda-index"),
            @DynamoDbAttribute("id_tienda")})
    private Long idTienda;

    @Getter(onMethod_ = {@DynamoDbSecondaryPartitionKey(indexNames = "id_categoria-index"),
            @DynamoDbAttribute("id_categoria")})
    private Long idCategoria;
}
