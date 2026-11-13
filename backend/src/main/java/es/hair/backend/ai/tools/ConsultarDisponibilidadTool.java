package es.hair.backend.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.hair.backend.ai.AiTool;
import es.hair.backend.ai.ToolContext;
import es.hair.backend.ai.port.DisponibilidadPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
@Profile("ia-stub")
public class ConsultarDisponibilidadTool implements AiTool {

    private static final LocalTime MEDIODIA = LocalTime.of(14, 0);

    private final ObjectMapper mapper;
    private final DisponibilidadPort disponibilidad;

    public ConsultarDisponibilidadTool(ObjectMapper mapper, DisponibilidadPort disponibilidad) {
        this.mapper = mapper;
        this.disponibilidad = disponibilidad;
    }

    @Override
    public String name() {
        return "consultar_disponibilidad";
    }

    @Override
    public String declarationJson() {
        return """
            {
              "name": "consultar_disponibilidad",
              "description": "Devuelve los huecos libres de 40 minutos de un día concreto. Úsala siempre antes de ofrecer horarios.",
              "parameters": {
                "type": "object",
                "properties": {
                  "fecha": {"type": "string", "description": "Fecha en formato YYYY-MM-DD"},
                  "franja": {"type": "string", "enum": ["MANANA", "TARDE", "TODO"],
                             "description": "MANANA: antes de las 14:00. TARDE: desde las 14:00. TODO: todo el día."}
                },
                "required": ["fecha"]
              }
            }
            """;
    }

    @Override
    public ObjectNode execute(JsonNode args, ToolContext ctx) {
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(args.path("fecha").asText(""));
        } catch (DateTimeParseException e) {
            return mapper.createObjectNode().put("error", "fecha_invalida");
        }
        String franja = args.path("franja").asText("TODO");

        List<LocalDateTime> libres = disponibilidad.huecosLibres(fecha).stream()
                .filter(h -> switch (franja) {
                    case "MANANA" -> h.toLocalTime().isBefore(MEDIODIA);
                    case "TARDE" -> !h.toLocalTime().isBefore(MEDIODIA);
                    default -> true;
                }).toList();

        // Los botones del frontend salen de aquí (dato real), no del texto del modelo.
        ctx.huecos().clear();
        ArrayNode horas = mapper.createArrayNode();
        for (LocalDateTime h : libres) {
            ctx.huecos().add(h.toString());
            horas.add(h.toLocalTime().toString());
        }
        ObjectNode resultado = mapper.createObjectNode();
        resultado.put("fecha", fecha.toString());
        resultado.set("huecos", horas);
        resultado.put("cantidad", libres.size());
        if (libres.isEmpty()) {
            resultado.put("nota", "No hay huecos libres ese día: puede estar cerrado, completo o bloqueado.");
        }
        return resultado;
    }
}
