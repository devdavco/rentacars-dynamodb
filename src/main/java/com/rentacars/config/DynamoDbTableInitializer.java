package com.rentacars.config;

import com.rentacars.repository.dynamo.DynamoTables;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Crea las tablas de DynamoDB al arrancar, solo si no existen
 * (reemplaza a script_bd.sql). Las claves de cada tabla e indice salen de las
 * anotaciones de los modelos; aqui solo se nombran los GSI.
 *
 * Se apaga con aws.dynamodb.create-tables=false (ej. en AWS, si las tablas
 * las crea otra herramienta).
 */
@Component
@ConditionalOnProperty(name = "aws.dynamodb.create-tables", havingValue = "true", matchIfMissing = true)
public class DynamoDbTableInitializer {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbTableInitializer.class);

    private final DynamoTables tablas;
    private final DynamoDbClient dynamoDbClient;

    public DynamoDbTableInitializer(DynamoTables tablas, DynamoDbClient dynamoDbClient) {
        this.tablas = tablas;
        this.dynamoDbClient = dynamoDbClient;
    }

    @PostConstruct
    public void crearTablas() {
        crearSiNoExiste(tablas.tiendas(), "ciudad-index");
        crearSiNoExiste(tablas.categorias(), "nombre-index");
        crearSiNoExiste(tablas.clientes(), "email-index");
        crearSiNoExiste(tablas.autos(), "id_tienda-index", "id_categoria-index");
        crearSiNoExiste(tablas.detallesAutos());
        crearSiNoExiste(tablas.alquileres(), "id_cliente-index", "id_auto-index", "estado-index");
        crearSiNoExiste(tablas.contadores());
        crearSiNoExiste(tablas.unicos());
    }

    private void crearSiNoExiste(DynamoDbTable<?> tabla, String... indices) {
        try {
            tabla.describeTable();
            log.info("Tabla DynamoDB '{}' ya existe", tabla.tableName());
            return;
        } catch (ResourceNotFoundException noExiste) {
            // hay que crearla
        }

        CreateTableEnhancedRequest.Builder request = CreateTableEnhancedRequest.builder();
        if (indices.length > 0) {
            request.globalSecondaryIndices(Arrays.stream(indices)
                    .map(nombre -> EnhancedGlobalSecondaryIndex.builder()
                            .indexName(nombre)
                            .projection(p -> p.projectionType(ProjectionType.ALL))
                            .build())
                    .toList());
        }
        // Sin provisionedThroughput el Enhanced Client usa PAY_PER_REQUEST (on-demand)
        tabla.createTable(request.build());
        dynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(tabla.tableName()));
        log.info("Tabla DynamoDB '{}' creada", tabla.tableName());
    }
}
