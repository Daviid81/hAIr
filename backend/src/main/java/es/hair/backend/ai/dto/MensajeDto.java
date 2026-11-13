package es.hair.backend.ai.dto;

/** rol: "USER" o "ASSISTANT". */
public record MensajeDto(String rol, String texto) {
}
