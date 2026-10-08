package com.rentacars.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

/**
 * Conexion a DynamoDB (reemplaza al datasource de PostgreSQL).
 *
 * Todo sale de application.properties:
 *   aws.dynamodb.region    -> region de AWS (us-east-1, ...)
 *   aws.dynamodb.endpoint  -> OPCIONAL. Solo para DynamoDB Local
 *                             (http://localhost:8000). Vacio = AWS real.
 *
 * Las credenciales NUNCA van en el codigo: DefaultCredentialsProvider las
 * busca en variables de entorno (AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY),
 * en el perfil de ~/.aws (AWS_PROFILE) o en el rol de la instancia/contenedor.
 */
@Configuration
public class DynamoDbConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(
            @Value("${aws.dynamodb.region}") String region,
            @Value("${aws.dynamodb.endpoint:}") String endpoint) {

        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build());

        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
