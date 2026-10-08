package com.rentacars.repository;

import com.rentacars.config.DynamoDbTableInitializer;
import com.rentacars.model.Cliente;
import com.rentacars.model.DetalleAuto;
import com.rentacars.repository.dynamo.DynamoTables;
import com.rentacars.repository.dynamo.GuardasUnicas;
import com.rentacars.repository.dynamo.IdGenerator;
import com.rentacars.repository.dynamo.Unico;
import java.math.BigDecimal;
import java.net.Socket;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Guardas de unicidad (email, placa) contra DynamoDB Local.
 * Se omite si DynamoDB Local no esta en localhost:8000 (docker compose up -d dynamodb-local).
 */
class GuardasUnicasDynamoLocalTest {

    private static ClienteRepository clientes;
    private static Detalle_autoRepository detalles;
    private static DynamoDbTable<Unico> unicos;

    @BeforeAll
    static void conectar() {
        assumeTrue(dynamoLocalDisponible(), "DynamoDB Local no esta levantado en localhost:8000");

        DynamoDbClient client = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:8000"))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local")))
                .build();
        DynamoDbEnhancedClient enhanced = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();

        // Prefijo propio para no tocar las tablas de la app
        DynamoTables tablas = new DynamoTables(enhanced, "test_");
        new DynamoDbTableInitializer(tablas, client).crearTablas();

        IdGenerator ids = new IdGenerator(client, tablas);
        GuardasUnicas guardas = new GuardasUnicas(enhanced, tablas);
        clientes = new ClienteRepository(tablas, ids, guardas);
        detalles = new Detalle_autoRepository(tablas, ids, guardas);
        unicos = tablas.unicos();
    }

    @Test
    void cambiarEmailMueveLaGuardaYNoCambiarloNoLaToca() {
        String emailA = unico("a@x.com");
        String emailB = unico("b@x.com");

        Cliente cliente = new Cliente(null, "Ana", emailA, "300", "4111");
        clientes.save(cliente);
        assertTrue(existeGuarda("EMAIL#" + emailA));

        // mismo email: la guarda sigue igual
        cliente.setTelefono("301");
        clientes.save(cliente);
        assertTrue(existeGuarda("EMAIL#" + emailA));

        // email nuevo: se borra la vieja y se crea la nueva en la misma transaccion
        cliente.setEmail(emailB);
        clientes.save(cliente);
        assertFalse(existeGuarda("EMAIL#" + emailA));
        assertTrue(existeGuarda("EMAIL#" + emailB));
        assertEquals(emailB, clientes.findById(cliente.getIdCliente()).orElseThrow().getEmail());

        // el email viejo quedo libre para otro cliente
        clientes.save(new Cliente(null, "Beto", emailA, "302", "4222"));
        assertTrue(existeGuarda("EMAIL#" + emailA));
    }

    @Test
    void cambiarAUnEmailOcupadoFallaYNoModificaNada() {
        String emailA = unico("a@x.com");
        String emailB = unico("b@x.com");
        clientes.save(new Cliente(null, "Ana", emailA, "300", "4111"));
        Cliente beto = clientes.save(new Cliente(null, "Beto", emailB, "301", "4222"));

        beto.setEmail(emailA);
        beto.setTelefono("999");
        assertThrows(DataIntegrityViolationException.class, () -> clientes.save(beto));

        Cliente guardado = clientes.findById(beto.getIdCliente()).orElseThrow();
        assertEquals(emailB, guardado.getEmail());
        assertEquals("301", guardado.getTelefono());
        assertTrue(existeGuarda("EMAIL#" + emailB));
    }

    @Test
    void cambiarPlacaMueveLaGuardaYBorrarLaLibera() {
        String placaA = unico("PA");
        String placaB = unico("PB");
        long idAuto = System.nanoTime();

        DetalleAuto detalle = new DetalleAuto(null, null, "M", "X", "2024", placaA,
                new BigDecimal("1000.00"), null, idAuto);
        detalles.save(detalle);
        assertTrue(existeGuarda("PLACA#" + placaA));

        detalle.setPrecioDia(new BigDecimal("2000.00"));
        detalles.save(detalle);
        assertTrue(existeGuarda("PLACA#" + placaA));

        detalle.setPlaca(placaB);
        detalles.save(detalle);
        assertFalse(existeGuarda("PLACA#" + placaA));
        assertTrue(existeGuarda("PLACA#" + placaB));

        detalles.delete(detalle);
        assertFalse(existeGuarda("PLACA#" + placaB));
        assertTrue(detalles.findByIdAuto(idAuto).isEmpty());
    }

    private static boolean existeGuarda(String valor) {
        return unicos.getItem(Key.builder().partitionValue(valor).build()) != null;
    }

    private static String unico(String base) {
        return UUID.randomUUID() + "-" + base;
    }

    private static boolean dynamoLocalDisponible() {
        try (Socket ignored = new Socket("localhost", 8000)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
