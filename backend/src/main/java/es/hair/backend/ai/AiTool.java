package es.hair.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Una herramienta que Gemini puede pedir al backend que ejecute. */
public interface AiTool {

    String name();

    /** Declaración JSON de la función (nombre, descripción y parámetros) en el formato de Gemini. */
    String declarationJson();

    ObjectNode execute(JsonNode args, ToolContext ctx);
}
