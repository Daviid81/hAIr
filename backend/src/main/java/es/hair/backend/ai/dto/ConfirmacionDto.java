package es.hair.backend.ai.dto;

/** Confirmación pendiente que el frontend muestra con los botones Confirmar / Cancelar (fase IA-2). */
public record ConfirmacionDto(String id, String tipo, String resumen) {
}
