package com.rentacars.repository;

import com.rentacars.model.DetalleAuto;
import com.rentacars.repository.dynamo.DynamoRepository;
import com.rentacars.repository.dynamo.DynamoTables;
import com.rentacars.repository.dynamo.GuardasUnicas;
import com.rentacars.repository.dynamo.IdGenerator;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Tabla "detalles_autos". Su PK es id_auto (relacion 1 a 1 con autos).
 * La placa es unica: save() y delete() mantienen la guarda PLACA#... en la
 * tabla "unicos" dentro de la misma transaccion (ver GuardasUnicas).
 */
@Repository
public class Detalle_autoRepository extends DynamoRepository<DetalleAuto> {

    private final GuardasUnicas guardasUnicas;

    public Detalle_autoRepository(DynamoTables tablas, IdGenerator idGenerator, GuardasUnicas guardasUnicas) {
        super(tablas.detallesAutos(), idGenerator, DynamoTables.DETALLES_AUTOS,
                DetalleAuto::getIdDetallesAutos, DetalleAuto::setIdDetallesAutos);
        this.guardasUnicas = guardasUnicas;
    }

    // HU-12 (Cardona): la ficha comercial de un auto. Como la PK es id_auto, es un GetItem directo.
    public Optional<DetalleAuto> findByIdAuto(Long idAuto) {
        return findById(idAuto);
    }

    /**
     * Si la placa cambia, en la misma TransactWriteItems se borra la guarda
     * vieja y se crea la nueva; si no cambia, la guarda no se toca.
     * Placa repetida -> DataIntegrityViolationException (409).
     */
    @Override
    public DetalleAuto save(DetalleAuto detalle) {
        String placaAnterior = findById(detalle.getIdAuto()).map(DetalleAuto::getPlaca).orElse(null);

        asignarId(detalle);
        guardasUnicas.guardar(table, detalle,
                GuardasUnicas.clave("PLACA", placaAnterior),
                GuardasUnicas.clave("PLACA", detalle.getPlaca()),
                DynamoTables.DETALLES_AUTOS + "#" + detalle.getIdAuto());
        return detalle;
    }

    @Override
    public void delete(DetalleAuto detalle) {
        guardasUnicas.borrar(table, detalle, GuardasUnicas.clave("PLACA", detalle.getPlaca()));
    }
}
