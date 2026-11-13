package es.hair.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 usa Jackson 3 por defecto y ya no crea el ObjectMapper clásico (Jackson 2).
 * El módulo de IA lo necesita para construir las peticiones a Gemini, así que se declara aquí.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper geminiObjectMapper() {
        return new ObjectMapper();
    }
}