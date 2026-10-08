package com.rentacars.repository;

import com.rentacars.model.Auto;
import com.rentacars.repository.dynamo.DynamoRepository;
import com.rentacars.repository.dynamo.DynamoTables;
import com.rentacars.repository.dynamo.IdGenerator;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Repository
public class AutoRepository extends DynamoRepository<Auto> {

    // HU-09: para resolver el filtro por ciudad (antes era un JOIN con tiendas)
    private final TiendaRepository tiendaRepository;

    public AutoRepository(DynamoTables tablas, IdGenerator idGenerator, TiendaRepository tiendaRepository) {
        super(tablas.autos(), idGenerator, DynamoTables.AUTOS, Auto::getIdAuto, Auto::setIdAuto);
        this.tiendaRepository = tiendaRepository;
    }

    // valida si la tienda tiene autos antes de borrarla
    public boolean existsByIdTienda(Long idTienda) {
        return existsInIndex("id_tienda-index", Key.builder().partitionValue(idTienda).build());
    }

    // valida si la categoria tiene autos antes de borrarla
    public boolean existsByIdCategoria(Long idCategoria) {
        return existsInIndex("id_categoria-index", Key.builder().partitionValue(idCategoria).build());
    }

    /**
     * HU-09 (Suarez): autos disponibles, con filtros opcionales de ciudad y categoria.
     *
     * En PostgreSQL era un JOIN autos-tiendas. En DynamoDB:
     *   - con ciudad: tiendas de esa ciudad (ciudad-index) y luego los autos de
     *     cada tienda (id_tienda-index), filtrando disponibilidad y categoria.
     *   - solo categoria: autos de esa categoria (id_categoria-index).
     *   - sin filtros: Scan de autos filtrando disponibilidad.
     */
    public List<Auto> buscarDisponibles(String ciudad, Long idCategoria) {
        Stream<Auto> autos;
        if (ciudad != null) {
            Expression filtro = filtroDisponibles(idCategoria);
            autos = tiendaRepository.findByCiudadIgnoreCase(ciudad).stream()
                    .flatMap(tienda -> queryIndex("id_tienda-index",
                            QueryConditional.keyEqualTo(Key.builder().partitionValue(tienda.getIdTienda()).build()),
                            filtro));
        } else if (idCategoria != null) {
            autos = queryIndex("id_categoria-index",
                    QueryConditional.keyEqualTo(Key.builder().partitionValue(idCategoria).build()),
                    filtroDisponibles(null));
        } else {
            autos = scan(filtroDisponibles(null));
        }
        return ordenar(autos);
    }

    private Expression filtroDisponibles(Long idCategoria) {
        Map<String, String> nombres = new HashMap<>(Map.of("#disp", "disponibilidad"));
        Map<String, AttributeValue> valores = new HashMap<>(Map.of(":si", AttributeValue.fromBool(true)));
        String expresion = "#disp = :si";
        if (idCategoria != null) {
            nombres.put("#cat", "id_categoria");
            valores.put(":cat", AttributeValue.fromN(idCategoria.toString()));
            expresion += " AND #cat = :cat";
        }
        return Expression.builder()
                .expression(expresion)
                .expressionNames(nombres)
                .expressionValues(valores)
                .build();
    }
}
