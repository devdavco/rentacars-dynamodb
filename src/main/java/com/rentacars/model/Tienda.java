package com.rentacars.model;

import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

/**
 * Tabla DynamoDB "tiendas".
 *
 *   PK:  id_tienda (N)
 *   GSI: ciudad-index -> PK ciudad_lower (S)   (HU-03: filtro por ciudad sin
 *                                               importar mayusculas)
 *
 * Las anotaciones de DynamoDB van en los GETTERS; con Lombok se ponen con
 * @Getter(onMethod_ = ...). @DynamoDbAttribute fija el nombre del atributo
 * en snake_case (los mismos nombres que tenian las columnas en PostgreSQL).
 */
@DynamoDbBean
@Getter
@Setter
@NoArgsConstructor           // Constructor vacio: new Tienda() -- el Enhanced Client lo exige
@AllArgsConstructor
public class Tienda {

    @Getter(onMethod_ = {@DynamoDbPartitionKey, @DynamoDbAttribute("id_tienda")})
    private Long idTienda;

    private String nombre;

    private String ciudad;

    private String direccion;

    /**
     * Copia de la ciudad en minusculas, solo para el indice ciudad-index.
     * Se calcula sola a partir de "ciudad", asi nunca queda desactualizada.
     * No sale en el JSON (los DTO no la tienen).
     */
    @DynamoDbSecondaryPartitionKey(indexNames = "ciudad-index")
    @DynamoDbAttribute("ciudad_lower")
    public String getCiudadLower() {
        return ciudad == null ? null : ciudad.toLowerCase(Locale.ROOT);
    }

    /** El Enhanced Client necesita un setter; el valor real se deriva de ciudad. */
    public void setCiudadLower(String ignorado) {
    }
}
