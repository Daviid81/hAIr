package es.hair.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Cliente REST de Gemini. La clave solo se lee de la configuración externa (variable de entorno). */
@Component
@Profile("ia-stub")
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final int timeoutSeconds;

    public GeminiClient(ObjectMapper mapper,
                        @Value("${gemini.api-key}") String apiKey,
                        @Value("${gemini.model}") String model,
                        @Value("${gemini.base-url}") String baseUrl,
                        @Value("${gemini.timeout-seconds:30}") int timeoutSeconds) {
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.timeoutSeconds = timeoutSeconds;
    }

    /** Devuelve el nodo "content" de la primera respuesta del modelo, tal cual lo envía Gemini. */
    public JsonNode generate(String system, ArrayNode contents, ArrayNode declarations) {
        ObjectNode body = mapper.createObjectNode();
        body.putObject("systemInstruction").putArray("parts").addObject().put("text", system);
        body.set("contents", contents);
        body.putArray("tools").addObject().set("functionDeclarations", declarations);

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/models/" + model + ":generateContent"))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() == 429) {
            log.warn("Gemini devolvió 429; se reintenta una vez");
            pause(2000);
            response = send(request);
        }
        if (response.statusCode() == 429) {
            throw new AiUnavailableException("Límite de uso de Gemini alcanzado", 60);
        }
        if (response.statusCode() >= 300) {
            // Solo se registra el estado: el cuerpo puede contener texto del cliente.
            log.error("Gemini respondió con estado {}", response.statusCode());
            throw new AiUnavailableException("Gemini devolvió el estado " + response.statusCode(), 30);
        }
        return extraerContenido(response.body());
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            log.error("Fallo de comunicación con Gemini: {}", e.getClass().getSimpleName());
            throw new AiUnavailableException("No se pudo contactar con Gemini", 30);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("Llamada a Gemini interrumpida", 30);
        }
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private JsonNode extraerContenido(String json) {
        try {
            JsonNode candidato = mapper.readTree(json).path("candidates").path(0);
            JsonNode content = candidato.path("content");
            if (!content.has("parts")) {
                log.warn("Gemini sin contenido. finishReason={}", candidato.path("finishReason").asText("?"));
                throw new AiUnavailableException("Respuesta vacía de Gemini", 30);
            }
            return content;
        } catch (IOException e) {
            throw new AiUnavailableException("Respuesta de Gemini ilegible", 30);
        }
    }
}
