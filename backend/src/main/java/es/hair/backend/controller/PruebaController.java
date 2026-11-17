package es.hair.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Indicamos que esta clase gestionará peticiones HTTP.
@RestController

// Establecemos la ruta general del controlador.
@RequestMapping("/api")
public class PruebaController {

    // Endpoint para comprobar la comunicación con React.
    @GetMapping("/prueba")
    public Map<String, String> comprobarConexion() {

        // Devolvemos un mensaje en formato JSON.
        return Map.of(
                "mensaje",
                "La conexión entre hAIr y el backend funciona correctamente."
        );
    }
}
