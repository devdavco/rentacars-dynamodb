package com.rentacars.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.swagger.v3.core.jackson.ModelResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sin esto, Swagger genera el schema de los DTOs con su propio ObjectMapper
 * interno (el de swagger-core), que no conoce
 * spring.jackson.property-naming-strategy=SNAKE_CASE. El resultado: Swagger
 * muestra y deja rellenar los campos en camelCase ("precioDia", "idTienda"),
 * pero la peticion real la procesa el ObjectMapper de Spring, que solo
 * reconoce snake_case ("precio_dia", "id_tienda"). Los campos camelCase
 * llegan en null, saltan las validaciones @NotBlank/@NotNull, y Swagger
 * responde 400 sin crear nada -- pareciendo un bug del backend cuando en
 * realidad el schema mostrado ya estaba mal.
 *
 * OJO: este proyecto no expone un bean ObjectMapper autowireable (con los
 * starters modulares de Spring Boot usados aqui, JacksonAutoConfiguration
 * no lo registra), asi que no se puede inyectar el ObjectMapper de la app.
 * En vez de eso, se crea uno nuevo con la MISMA estrategia SNAKE_CASE que
 * application.properties, solo para que swagger-core genere el schema con
 * los nombres correctos.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public ModelResolver modelResolver() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return new ModelResolver(mapper);
    }
}
