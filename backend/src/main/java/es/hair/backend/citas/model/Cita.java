package es.hair.backend.citas.model;

import java.time.LocalDateTime;

public record Cita(
        Integer id,
        Integer usuarioId,
        LocalDateTime inicio,
        LocalDateTime fin,
        EstadoCita estado,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
