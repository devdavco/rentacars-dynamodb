package com.rentacars.repository;

import com.rentacars.model.Categoria;
import com.rentacars.repository.dynamo.DynamoRepository;
import com.rentacars.repository.dynamo.DynamoTables;
import com.rentacars.repository.dynamo.IdGenerator;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Repository
public class CategoriaRepository extends DynamoRepository<Categoria> {

    public CategoriaRepository(DynamoTables tablas, IdGenerator idGenerator) {
        super(tablas.categorias(), idGenerator, DynamoTables.CATEGORIAS,
                Categoria::getIdCategoria, Categoria::setIdCategoria);
    }

    // HU-06: consulta el indice nombre-index
    public boolean existsByNombre(String nombre) {
        return existsInIndex("nombre-index", Key.builder().partitionValue(nombre).build());
    }
}
