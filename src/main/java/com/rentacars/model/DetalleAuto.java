package com.rentacars.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * Tabla DynamoDB "detalles_autos".
 *
 *   PK: id_auto (N)   <- OJO: la PK es el id del AUTO, no id_detalles_autos.
 *
 * La relacion con Auto es 1 a 1 y siempre se busca "el detalle del auto X"
 * (findByIdAuto). Usar id_auto como PK convierte esa busqueda en un GetItem
 * directo, sin indices. id_detalles_autos se conserva como atributo normal.
 *
 * La placa es UNICA: Detalle_autoRepository lo garantiza con una guarda en
 * la tabla "unicos" (DynamoDB no tiene UNIQUE).
 *
 * SOBRE BigDecimal:
 * precio_dia y oferta_porcentaje eran DECIMAL(10,2) en PostgreSQL. En DynamoDB
 * son numeros (N) y Decimal2Converter los devuelve con 2 decimales.
 * En Java se usa BigDecimal (NO double). Con dinero, double da errores de
 * redondeo. La diferencia practica es que no se usan los operadores * - + :
 *
 *     double:     total = precio * dias;
 *     BigDecimal: total = precio.multiply(BigDecimal.valueOf(dias));
 *
 * Operaciones que van a necesitar:
 *     a.add(b)                    a + b
 *     a.subtract(b)               a - b
 *     a.multiply(b)               a * b
 *     a.divide(b, 2, RoundingMode.HALF_UP)   a / b con 2 decimales
 *     BigDecimal.valueOf(100)     convertir un numero normal a BigDecimal
 *
 * Ejemplo real, el calculo de HU-12 (precio con oferta):
 *
 *     BigDecimal oferta = detalle.getOfertaPorcentaje();
 *     if (oferta == null) oferta = BigDecimal.ZERO;   // el atributo admite null
 *
 *     BigDecimal descuento = detalle.getPrecioDia()
 *             .multiply(oferta)
 *             .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
 *
 *     BigDecimal precioConOferta = detalle.getPrecioDia().subtract(descuento);
 */
@DynamoDbBean
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleAuto {

    @Getter(onMethod_ = @DynamoDbAttribute("id_detalles_autos"))
    private Long idDetallesAutos;

    private String imagen;

    private String modelo;

    private String marca;

    private String anio;

    private String placa;

    // Debe ser > 0 (lo valida CreateAutoRequest con @Positive)
    @Getter(onMethod_ = {@DynamoDbAttribute("precio_dia"), @DynamoDbConvertedBy(Decimal2Converter.class)})
    private BigDecimal precioDia;

    // PUEDE SER NULL (auto sin oferta). Siempre revisar null antes de calcular.
    // Debe estar entre 0 y 100.
    @Getter(onMethod_ = {@DynamoDbAttribute("oferta_porcentaje"), @DynamoDbConvertedBy(Decimal2Converter.class)})
    private BigDecimal ofertaPorcentaje;

    @Getter(onMethod_ = {@DynamoDbPartitionKey, @DynamoDbAttribute("id_auto")})
    private Long idAuto;
}
