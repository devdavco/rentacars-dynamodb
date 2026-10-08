package com.rentacars.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

/**
 * Tabla DynamoDB "clientes".
 *
 *   PK:  id_cliente (N)
 *   GSI: email-index -> PK email (S)
 *
 * OJO (HU-14, HU-15, HU-16): la tarjeta de credito SE GUARDA aqui,
 * pero NUNCA se devuelve completa en un DTO de respuesta.
 * En HU-16 se muestra enmascarada: ************1111
 *
 * El email es UNICO: ClienteRepository lo garantiza con una guarda en la
 * tabla "unicos" (DynamoDB no tiene UNIQUE). HU-14 igual lo valida antes con
 * existsByEmail() para responder un 400 con el mensaje "El email ya existe".
 */
@DynamoDbBean
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Getter(onMethod_ = {@DynamoDbPartitionKey, @DynamoDbAttribute("id_cliente")})
    private Long idCliente;

    private String nombre;

    @Getter(onMethod_ = @DynamoDbSecondaryPartitionKey(indexNames = "email-index"))
    private String email;

    private String telefono;

    // Admite null, aunque HU-14 la exige al registrar
    @Getter(onMethod_ = @DynamoDbAttribute("tarjeta_credito"))
    private String tarjetaCredito;
}
