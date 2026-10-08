package com.rentacars.repository;

import com.rentacars.model.Cliente;
import com.rentacars.repository.dynamo.DynamoRepository;
import com.rentacars.repository.dynamo.DynamoTables;
import com.rentacars.repository.dynamo.GuardasUnicas;
import com.rentacars.repository.dynamo.IdGenerator;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.Key;

/**
 * Tabla "clientes". El email es unico: save() mantiene la guarda EMAIL#... en
 * la tabla "unicos" dentro de la misma transaccion (ver GuardasUnicas).
 */
@Repository
public class ClienteRepository extends DynamoRepository<Cliente> {

    private final GuardasUnicas guardasUnicas;

    public ClienteRepository(DynamoTables tablas, IdGenerator idGenerator, GuardasUnicas guardasUnicas) {
        super(tablas.clientes(), idGenerator, DynamoTables.CLIENTES, Cliente::getIdCliente, Cliente::setIdCliente);
        this.guardasUnicas = guardasUnicas;
    }

    // HU-14 (Murcia): valida email duplicado antes de registrar
    public boolean existsByEmail(String email) {
        return existsInIndex("email-index", Key.builder().partitionValue(email).build());
    }

    /**
     * Si el email cambia, en la misma TransactWriteItems se borra la guarda
     * vieja y se crea la nueva; si no cambia, la guarda no se toca.
     * Email repetido -> DataIntegrityViolationException (409).
     */
    @Override
    public Cliente save(Cliente cliente) {
        String emailAnterior = cliente.getIdCliente() == null
                ? null
                : findById(cliente.getIdCliente()).map(Cliente::getEmail).orElse(null);

        asignarId(cliente);
        guardasUnicas.guardar(table, cliente,
                GuardasUnicas.clave("EMAIL", emailAnterior),
                GuardasUnicas.clave("EMAIL", cliente.getEmail()),
                DynamoTables.CLIENTES + "#" + cliente.getIdCliente());
        return cliente;
    }

    @Override
    public void delete(Cliente cliente) {
        guardasUnicas.borrar(table, cliente, GuardasUnicas.clave("EMAIL", cliente.getEmail()));
    }
}
