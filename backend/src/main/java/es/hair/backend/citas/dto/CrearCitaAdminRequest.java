package es.hair.backend.citas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

public record CrearCitaAdminRequest(
        @NotNull
        @Positive
        Integer usuarioId,
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate fecha,
        @NotNull
        @JsonFormat(pattern = "HH:mm")
        LocalTime horaInicio
) {
}
