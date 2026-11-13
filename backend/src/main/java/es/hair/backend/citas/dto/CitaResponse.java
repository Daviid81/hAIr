package es.hair.backend.citas.dto;

import es.hair.backend.citas.model.EstadoCita;

import java.time.OffsetDateTime;

public record CitaResponse(
        Integer id,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        EstadoCita estado,
        boolean cancelable,
        OffsetDateTime limiteCancelacion
) {
}
