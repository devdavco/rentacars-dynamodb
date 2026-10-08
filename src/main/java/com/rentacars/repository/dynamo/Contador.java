package com.rentacars.repository.dynamo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * Item de la tabla "contadores": el ultimo id entregado por entidad
 * (reemplaza a las secuencias BIGSERIAL de PostgreSQL). Ver IdGenerator.
 */
@DynamoDbBean
@Getter
@Setter
@NoArgsConstructor
public class Contador {

    @Getter(onMethod_ = @DynamoDbPartitionKey)
    private String entidad;

    private Long valor;
}
