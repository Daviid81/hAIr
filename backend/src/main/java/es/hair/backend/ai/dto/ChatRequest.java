package es.hair.backend.ai.dto;

import java.util.List;

public record ChatRequest(List<MensajeDto> mensajes) {
}
