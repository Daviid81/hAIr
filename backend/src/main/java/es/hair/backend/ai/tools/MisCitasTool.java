package es.hair.backend.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.hair.backend.ai.AiTool;
import es.hair.backend.ai.ToolContext;
import es.hair.backend.ai.port.CitaResumen;
import es.hair.backend.ai.port.CitasPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("ia-stub")
public class MisCitasTool implements AiTool {

    private final ObjectMapper mapper;
    private final CitasPort citas;

    public MisCitasTool(ObjectMapper mapper, CitasPort citas) {
        this.mapper = mapper;
        this.citas = citas;
    }

    @Override
    public String name() {
        return "mis_citas";
    }

    @Override
    public String declarationJson() {
        return """
            {
              "name": "mis_citas",
              "description": "Devuelve las citas futuras confirmadas del cliente que está conversando. Úsala cuando pregunte por su cita."
            }
            """;
    }

    @Override
    public ObjectNode execute(JsonNode args, ToolContext ctx) {
        ArrayNode lista = mapper.createArrayNode();
        // usuarioId: sale del contexto (JWT), nunca de los argumentos que genera el modelo.
        for (CitaResumen c : citas.citasProximas(ctx.usuarioId())) {
            ObjectNode n = lista.addObject();
            n.put("id", c.id());
            n.put("inicio", c.inicio().toString());
            n.put("estado", c.estado());
            n.put("cancelable", c.cancelable());
            n.put("limiteCancelacion", c.limiteCancelacion().toString());
        }
        ObjectNode resultado = mapper.createObjectNode();
        resultado.set("citas", lista);
        resultado.put("cantidad", lista.size());
        return resultado;
    }
}
