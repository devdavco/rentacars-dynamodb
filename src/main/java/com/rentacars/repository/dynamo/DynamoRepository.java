package com.rentacars.repository.dynamo;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

/**
 * Base de los repositorios: ofrece los mismos metodos que usaban los services
 * con JpaRepository (findById, findAll, save, delete), mas ayudas para
 * consultar indices (GSI).
 *
 * Las listas se devuelven ordenadas por id, como las devolvia PostgreSQL en
 * la practica (DynamoDB no garantiza orden en un Scan).
 */
public abstract class DynamoRepository<T> {

    protected final DynamoDbTable<T> table;
    private final IdGenerator idGenerator;
    private final String contador;
    private final Function<T, Long> getId;
    private final BiConsumer<T, Long> setId;

    protected DynamoRepository(DynamoDbTable<T> table, IdGenerator idGenerator, String contador,
                               Function<T, Long> getId, BiConsumer<T, Long> setId) {
        this.table = table;
        this.idGenerator = idGenerator;
        this.contador = contador;
        this.getId = getId;
        this.setId = setId;
    }

    public Optional<T> findById(Long id) {
        return Optional.ofNullable(table.getItem(Key.builder().partitionValue(id).build()));
    }

    public List<T> findAll() {
        return ordenar(table.scan().items().stream());
    }

    /** Inserta o reemplaza. Si el id viene null, lo asigna con el contador (como BIGSERIAL). */
    public T save(T item) {
        asignarId(item);
        table.putItem(item);
        return item;
    }

    public void delete(T item) {
        table.deleteItem(item);
    }

    protected void asignarId(T item) {
        if (getId.apply(item) == null) {
            setId.accept(item, idGenerator.siguiente(contador));
        }
    }

    /** Todos los items de un GSI con esa clave (recorre todas las paginas). */
    protected Stream<T> queryIndex(String indice, QueryConditional condicion, Expression filtro) {
        return table.index(indice)
                .query(QueryEnhancedRequest.builder()
                        .queryConditional(condicion)
                        .filterExpression(filtro)
                        .build())
                .stream()
                .flatMap(pagina -> pagina.items().stream());
    }

    protected Stream<T> queryIndex(String indice, Key clave) {
        return queryIndex(indice, QueryConditional.keyEqualTo(clave), null);
    }

    /** Equivalente a existsBy...: basta con leer 1 item del indice. */
    protected boolean existsInIndex(String indice, Key clave) {
        return table.index(indice)
                .query(QueryEnhancedRequest.builder()
                        .queryConditional(QueryConditional.keyEqualTo(clave))
                        .limit(1)
                        .build())
                .stream()
                .findFirst()
                .map(Page::items)
                .map(items -> !items.isEmpty())
                .orElse(false);
    }

    protected Stream<T> scan(Expression filtro) {
        return table.scan(ScanEnhancedRequest.builder().filterExpression(filtro).build())
                .items().stream();
    }

    protected List<T> ordenar(Stream<T> items) {
        return items.sorted(Comparator.comparing(getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
