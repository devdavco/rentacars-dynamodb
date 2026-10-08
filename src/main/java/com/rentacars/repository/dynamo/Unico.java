package com.rentacars.repository.dynamo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * Item de la tabla "unicos": reemplaza las restricciones UNIQUE de PostgreSQL.
 *
 * Cada valor que debe ser unico se guarda aqui como clave, por ejemplo
 * "EMAIL#carlos@email.com" o "PLACA#ABC-001". Como la clave de una tabla no
 * se puede repetir, insertarla con attribute_not_exists(valor) falla si otro
 * registro ya la tiene. Ver GuardasUnicas.
 */
@DynamoDbBean
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Unico {

    @Getter(onMethod_ = @DynamoDbPartitionKey)
    private String valor;

    /** Quien es el dueno del valor (ej. "clientes#3"). Solo informativo. */
    private String referencia;
}
