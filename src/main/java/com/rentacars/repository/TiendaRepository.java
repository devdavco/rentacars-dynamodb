package com.rentacars.repository;

import com.rentacars.model.Tienda;
import com.rentacars.repository.dynamo.DynamoRepository;
import com.rentacars.repository.dynamo.DynamoTables;
import com.rentacars.repository.dynamo.IdGenerator;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.List;
import java.util.Locale;

/**
 * Tabla "tiendas". Hereda findById, findAll, save y delete de DynamoRepository.
 */
@Repository
public class TiendaRepository extends DynamoRepository<Tienda> {

    public TiendaRepository(DynamoTables tablas, IdGenerator idGenerator) {
        super(tablas.tiendas(), idGenerator, DynamoTables.TIENDAS, Tienda::getIdTienda, Tienda::setIdTienda);
    }

    // HU-03 (Arango): filtro por ciudad ignorando mayusculas/minusculas.
    // Consulta el indice ciudad-index, que guarda la ciudad en minusculas.
    public List<Tienda> findByCiudadIgnoreCase(String ciudad) {
        if (ciudad.isEmpty()) {
            return List.of(); // DynamoDB no acepta claves vacias; ninguna tienda tiene ciudad ""
        }
        Key clave = Key.builder().partitionValue(ciudad.toLowerCase(Locale.ROOT)).build();
        return ordenar(queryIndex("ciudad-index", clave));
    }
}
