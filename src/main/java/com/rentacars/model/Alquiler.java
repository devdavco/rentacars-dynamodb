package com.rentacars.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

/**
 * Tabla DynamoDB "alquileres".
 *
 *   PK:  id_alquiler (N)
 *   GSI: id_cliente-index -> PK id_cliente (N), SK fecha_inicio (S)  (HU-20)
 *   GSI: id_auto-index    -> PK id_auto (N)                          (HU-13: 409)
 *   GSI: estado-index     -> PK estado (S), SK fecha_fin (S)         (HU-21)
 *
 * Las fechas se guardan como texto ISO (2025-07-05), que ordena igual que la
 * fecha: por eso "fecha_fin >= hoy" se resuelve con una Query sobre estado-index.
 *
 * SOBRE EL CAMPO estado:
 * Solo admite "ACTIVO" o "CERRADO".
 *   - HU-18 crea el alquiler con estado = "ACTIVO"
 *   - HU-24 lo pasa a "CERRADO" al registrar la devolucion
 *   - HU-21 lista solo los que estan en "ACTIVO"
 * DynamoDB no tiene CHECKs: estas reglas (y fecha_fin >= fecha_inicio,
 * precio_total >= 0) las garantizan el service y el mapper.
 *
 * precio_total era DECIMAL(10,2) -> BigDecimal en Java (ver DetalleAuto
 * para los ejemplos de como se hacen los calculos).
 */
@DynamoDbBean
@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alquiler {

    @Getter(onMethod_ = {@DynamoDbPartitionKey, @DynamoDbAttribute("id_alquiler")})
    private Long idAlquiler;

    @Getter(onMethod_ = {@DynamoDbSecondaryPartitionKey(indexNames = "id_cliente-index"),
            @DynamoDbAttribute("id_cliente")})
    private Long idCliente;

    @Getter(onMethod_ = {@DynamoDbSecondaryPartitionKey(indexNames = "id_auto-index"),
            @DynamoDbAttribute("id_auto")})
    private Long idAuto;

    @Getter(onMethod_ = {@DynamoDbSecondarySortKey(indexNames = "id_cliente-index"),
            @DynamoDbAttribute("fecha_inicio")})
    private LocalDate fechaInicio;

    @Getter(onMethod_ = {@DynamoDbSecondarySortKey(indexNames = "estado-index"),
            @DynamoDbAttribute("fecha_fin")})
    private LocalDate fechaFin;

    @Getter(onMethod_ = {@DynamoDbAttribute("precio_total"), @DynamoDbConvertedBy(Decimal2Converter.class)})
    private BigDecimal precioTotal;

    @Getter(onMethod_ = @DynamoDbAttribute("ciudad_retirada"))
    private String ciudadRetirada;

    @Getter(onMethod_ = @DynamoDbAttribute("ciudad_devolucion"))
    private String ciudadDevolucion;

    /** Solo "ACTIVO" o "CERRADO" */
    @Getter(onMethod_ = @DynamoDbSecondaryPartitionKey(indexNames = "estado-index"))
    private String estado;
}
