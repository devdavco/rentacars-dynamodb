package com.rentacars.repository.dynamo;

import java.util.Map;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

/**
 * Genera ids Long consecutivos, como hacia BIGSERIAL en PostgreSQL.
 *
 * "ADD valor :uno" es atomico en DynamoDB: aunque dos peticiones lleguen al
 * mismo tiempo, cada una recibe un numero distinto. Si el contador no existe
 * todavia, DynamoDB lo crea en 0 y le suma 1.
 */
@Component
public class IdGenerator {

    private final DynamoDbClient dynamoDbClient;
    private final String tablaContadores;

    public IdGenerator(DynamoDbClient dynamoDbClient, DynamoTables tablas) {
        this.dynamoDbClient = dynamoDbClient;
        this.tablaContadores = tablas.contadores().tableName();
    }

    public Long siguiente(String entidad) {
        Map<String, AttributeValue> atributos = dynamoDbClient.updateItem(r -> r
                        .tableName(tablaContadores)
                        .key(Map.of("entidad", AttributeValue.fromS(entidad)))
                        .updateExpression("ADD valor :uno")
                        .expressionAttributeValues(Map.of(":uno", AttributeValue.fromN("1")))
                        .returnValues(ReturnValue.UPDATED_NEW))
                .attributes();
        return Long.valueOf(atributos.get("valor").n());
    }
}
