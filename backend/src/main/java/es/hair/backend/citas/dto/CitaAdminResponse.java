package es.hair.backend.citas.dto;

import es.hair.backend.citas.model.EstadoCita;

import java.time.OffsetDateTime;

public record CitaAdminResponse(
        Integer id,
        Integer usuarioId,
        String nombre,
        String apellidos,
        String telefono,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        EstadoCita estado
) {
}
