package com.rentacars.model;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

/**
 * Tabla DynamoDB "categorias". Clasifica los autos (SUV, Sedan, Camioneta...).
 *
 *   PK:  id_categoria (N)
 *   GSI: nombre-index -> PK nombre (S)
 *
 * OJO HU-06: el nombre NO es unico a nivel de base de datos (igual que en
 * PostgreSQL). La regla "no se pueden registrar dos categorias con el mismo
 * nombre" la valida CategoriaServiceImpl con existsByNombre(), que consulta
 * el indice nombre-index.
 */
@DynamoDbBean
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Getter(onMethod_ = {@DynamoDbPartitionKey, @DynamoDbAttribute("id_categoria")})
    private Long idCategoria;

    @Getter(onMethod_ = @DynamoDbSecondaryPartitionKey(indexNames = "nombre-index"))
    private String nombre;

    private String descripcion;
}
