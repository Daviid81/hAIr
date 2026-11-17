package es.hair.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

// Definimos la información general de la API REST.
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "hAIr API",
                version = "1.0.0",
                description = "API REST del gestor de citas de peluquería hAIr"
        )
)
public class OpenApiConfig {
}
