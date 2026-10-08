package com.rentacars.repository.dynamo;

import com.rentacars.model.Alquiler;
import com.rentacars.model.Auto;
import com.rentacars.model.Categoria;
import com.rentacars.model.Cliente;
import com.rentacars.model.DetalleAuto;
import com.rentacars.model.Tienda;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchemaParams;

import java.lang.invoke.MethodHandles;

/**
 * Las 8 tablas de DynamoDB en un solo lugar.
 *
 * El nombre real de cada tabla es prefijo + nombre (aws.dynamodb.table-prefix),
 * por si varios ambientes comparten la misma cuenta de AWS.
 */
@Component
@Getter
@Accessors(fluent = true)
public class DynamoTables {

    // Nombres base. Tambien se usan como nombre de cada contador de ids.
    public static final String TIENDAS = "tiendas";
    public static final String CATEGORIAS = "categorias";
    public static final String CLIENTES = "clientes";
    public static final String AUTOS = "autos";
    public static final String DETALLES_AUTOS = "detalles_autos";
    public static final String ALQUILERES = "alquileres";
    public static final String CONTADORES = "contadores";
    public static final String UNICOS = "unicos";

    private final DynamoDbTable<Tienda> tiendas;
    private final DynamoDbTable<Categoria> categorias;
    private final DynamoDbTable<Cliente> clientes;
    private final DynamoDbTable<Auto> autos;
    private final DynamoDbTable<DetalleAuto> detallesAutos;
    private final DynamoDbTable<Alquiler> alquileres;
    private final DynamoDbTable<Contador> contadores;
    private final DynamoDbTable<Unico> unicos;

    public DynamoTables(DynamoDbEnhancedClient enhancedClient,
                        @Value("${aws.dynamodb.table-prefix:}") String prefijo) {
        this.tiendas = enhancedClient.table(prefijo + TIENDAS, esquema(Tienda.class));
        this.categorias = enhancedClient.table(prefijo + CATEGORIAS, esquema(Categoria.class));
        this.clientes = enhancedClient.table(prefijo + CLIENTES, esquema(Cliente.class));
        this.autos = enhancedClient.table(prefijo + AUTOS, esquema(Auto.class));
        this.detallesAutos = enhancedClient.table(prefijo + DETALLES_AUTOS, esquema(DetalleAuto.class));
        this.alquileres = enhancedClient.table(prefijo + ALQUILERES, esquema(Alquiler.class));
        this.contadores = enhancedClient.table(prefijo + CONTADORES, esquema(Contador.class));
        this.unicos = enhancedClient.table(prefijo + UNICOS, esquema(Unico.class));
    }

    /**
     * Esquema a partir de las anotaciones del modelo. Se le pasa un lookup
     * propio para que funcione con spring-boot-devtools (que carga las clases
     * del proyecto con otro classloader; sin esto da ClassCastException).
     */
    private static <T> TableSchema<T> esquema(Class<T> clase) {
        return TableSchema.fromBean(BeanTableSchemaParams.builder(clase)
                .lookup(MethodHandles.lookup())
                .build());
    }
}
