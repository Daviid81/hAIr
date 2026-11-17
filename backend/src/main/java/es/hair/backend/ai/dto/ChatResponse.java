package es.hair.backend.ai.dto;

import java.util.List;

public record ChatResponse(String respuesta, List<String> huecos, ConfirmacionDto confirmacionPendiente) {
}
