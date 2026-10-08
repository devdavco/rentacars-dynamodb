package com.rentacars.repository;

import com.rentacars.model.Alquiler;
import com.rentacars.repository.dynamo.DynamoRepository;
import com.rentacars.repository.dynamo.DynamoTables;
import com.rentacars.repository.dynamo.IdGenerator;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.time.LocalDate;
import java.util.List;

@Repository
public class AlquilerRepository extends DynamoRepository<Alquiler> {

    public AlquilerRepository(DynamoTables tablas, IdGenerator idGenerator) {
        super(tablas.alquileres(), idGenerator, DynamoTables.ALQUILERES,
                Alquiler::getIdAlquiler, Alquiler::setIdAlquiler);
    }

    // El mismo conversor que usa el Enhanced Client al guardar fecha_fin
    private static final AttributeConverter<LocalDate> CONVERSOR_FECHA =
            AttributeConverterProvider.defaultProvider().converterFor(EnhancedType.of(LocalDate.class));

    // valida si el auto tiene alquileres (indice id_auto-index)
    public boolean existsByIdAuto(Long idAuto) {
        return existsInIndex("id_auto-index", Key.builder().partitionValue(idAuto).build());
    }

    // HU-20 (Pedroza): historial de alquileres de un cliente (indice id_cliente-index)
    public List<Alquiler> findByIdCliente(Long idCliente) {
        return ordenar(queryIndex("id_cliente-index", Key.builder().partitionValue(idCliente).build()));
    }

    // HU-21 (Pedroza): activo = estado ACTIVO y fecha_fin >= hoy (regla del backlog).
    // Indice estado-index: PK estado, SK fecha_fin (texto ISO, ordena como fecha).
    public List<Alquiler> findByEstadoAndFechaFinGreaterThanEqual(String estado, LocalDate hoy) {
        Key desde = Key.builder()
                .partitionValue(estado)
                .sortValue(CONVERSOR_FECHA.transformFrom(hoy))
                .build();
        return ordenar(queryIndex("estado-index", QueryConditional.sortGreaterThanOrEqualTo(desde), null));
    }
}
