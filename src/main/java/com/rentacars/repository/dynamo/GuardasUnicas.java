package com.rentacars.repository.dynamo;

import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/**
 * Reemplazo de UNIQUE (email de clientes, placa de detalles_autos).
 *
 * El registro y su guarda en la tabla "unicos" se escriben en UNA SOLA
 * TransactWriteItems: o se guardan los dos, o ninguno.
 *
 *   - Registro nuevo:          put registro + put guarda nueva (attribute_not_exists)
 *   - Cambio del valor unico:  put registro + delete guarda vieja + put guarda nueva
 *   - Valor sin cambios:       put registro normal, la guarda no se toca
 *
 * Si la guarda nueva ya existe (valor repetido), se lanza
 * DataIntegrityViolationException, que GlobalExceptionHandler convierte en
 * 409, igual que hacia la restriccion UNIQUE de PostgreSQL.
 */
@Component
public class GuardasUnicas {

    private static final Expression NO_EXISTE = Expression.builder()
            .expression("attribute_not_exists(valor)")
            .build();

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<Unico> unicos;

    public GuardasUnicas(DynamoDbEnhancedClient enhancedClient, DynamoTables tablas) {
        this.enhancedClient = enhancedClient;
        this.unicos = tablas.unicos();
    }

    /** Arma la clave de la guarda, ej. clave("EMAIL", "a@b.com") -> "EMAIL#a@b.com" */
    public static String clave(String tipo, String valor) {
        return valor == null ? null : tipo + "#" + valor;
    }

    /**
     * Guarda el registro y mantiene su guarda.
     *
     * @param guardaVieja clave que tenia el registro antes (null si es nuevo)
     * @param guardaNueva clave que tiene ahora
     * @param referencia  dueno de la guarda, solo informativo (ej. "clientes#3")
     */
    public <T> void guardar(DynamoDbTable<T> tabla, T registro,
                            String guardaVieja, String guardaNueva, String referencia) {
        if (Objects.equals(guardaVieja, guardaNueva)) {
            tabla.putItem(registro);
            return;
        }

        TransactWriteItemsEnhancedRequest.Builder tx = TransactWriteItemsEnhancedRequest.builder()
                .addPutItem(tabla, registro);
        if (guardaVieja != null) {
            tx.addDeleteItem(unicos, Key.builder().partitionValue(guardaVieja).build());
        }
        if (guardaNueva != null) {
            tx.addPutItem(unicos, TransactPutItemEnhancedRequest.builder(Unico.class)
                    .item(new Unico(guardaNueva, referencia))
                    .conditionExpression(NO_EXISTE)
                    .build());
        }
        ejecutar(tx.build(), guardaNueva);
    }

    /** Borra el registro y libera su guarda en la misma transaccion. */
    public <T> void borrar(DynamoDbTable<T> tabla, T registro, String guarda) {
        TransactWriteItemsEnhancedRequest.Builder tx = TransactWriteItemsEnhancedRequest.builder()
                .addDeleteItem(tabla, registro);
        if (guarda != null) {
            tx.addDeleteItem(unicos, Key.builder().partitionValue(guarda).build());
        }
        ejecutar(tx.build(), guarda);
    }

    private void ejecutar(TransactWriteItemsEnhancedRequest request, String guarda) {
        try {
            enhancedClient.transactWriteItems(request);
        } catch (TransactionCanceledException ex) {
            boolean duplicado = ex.hasCancellationReasons() && ex.cancellationReasons().stream()
                    .anyMatch(r -> "ConditionalCheckFailed".equals(r.code()));
            if (duplicado) {
                throw new DataIntegrityViolationException("Valor unico repetido: " + guarda, ex);
            }
            throw ex;
        }
    }
}
