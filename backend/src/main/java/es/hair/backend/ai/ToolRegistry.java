package es.hair.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lista cerrada de herramientas disponibles para la IA. */
@Component
@Profile("ia-stub")
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, AiTool> porNombre = new LinkedHashMap<>();
    private final ObjectMapper mapper;

    public ToolRegistry(List<AiTool> herramientas, ObjectMapper mapper) {
        this.mapper = mapper;
        herramientas.forEach(t -> porNombre.put(t.name(), t));
    }

    public ArrayNode declaraciones() {
        ArrayNode array = mapper.createArrayNode();
        for (AiTool t : porNombre.values()) {
            try {
                array.add(mapper.readTree(t.declarationJson()));
            } catch (IOException e) {
                throw new IllegalStateException("Declaración inválida en la herramienta " + t.name(), e);
            }
        }
        return array;
    }

    public ObjectNode ejecutar(String nombre, JsonNode args, ToolContext ctx) {
        AiTool tool = porNombre.get(nombre);
        if (tool == null) {
            return mapper.createObjectNode().put("error", "herramienta_desconocida");
        }
        try {
            return tool.execute(args, ctx);
        } catch (RuntimeException e) {
            log.error("Fallo en la herramienta {}", nombre, e);
            return mapper.createObjectNode().put("error", "fallo_interno");
        }
    }
}
