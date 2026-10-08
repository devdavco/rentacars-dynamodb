package com.rentacars;

import java.net.Socket;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Levanta el contexto completo contra DynamoDB Local (nunca contra AWS real),
 * con prefijo "test_" para no tocar las tablas de la app.
 * Se omite si DynamoDB Local no esta en localhost:8000.
 */
@SpringBootTest(properties = {
        "aws.dynamodb.endpoint=http://localhost:8000",
        "aws.dynamodb.region=us-east-1",
        "aws.dynamodb.table-prefix=test_"
})
class RentacarsApplicationTests {

	@BeforeAll
	static void requiereDynamoLocal() {
		boolean disponible;
		try (Socket ignored = new Socket("localhost", 8000)) {
			disponible = true;
		} catch (Exception e) {
			disponible = false;
		}
		assumeTrue(disponible, "DynamoDB Local no esta levantado en localhost:8000");

		// DynamoDB Local no valida credenciales, pero el SDK exige que existan
		if (System.getProperty("aws.accessKeyId") == null) {
			System.setProperty("aws.accessKeyId", "local");
			System.setProperty("aws.secretAccessKey", "local");
		}
	}

	@Test
	void contextLoads() {
	}

}
