package com.rentacars.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Equivalente a DECIMAL(10,2) de PostgreSQL.
 *
 * DynamoDB guarda los numeros sin ceros a la derecha (80000.00 -> 80000).
 * Al leer se devuelve la escala 2, para que el JSON siga saliendo
 * igual que con PostgreSQL ("precio_dia": 80000.00).
 */
public class Decimal2Converter implements AttributeConverter<BigDecimal> {

    @Override
    public AttributeValue transformFrom(BigDecimal input) {
        return AttributeValue.fromN(input.toPlainString());
    }

    @Override
    public BigDecimal transformTo(AttributeValue input) {
        return new BigDecimal(input.n()).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public EnhancedType<BigDecimal> type() {
        return EnhancedType.of(BigDecimal.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.N;
    }
}
