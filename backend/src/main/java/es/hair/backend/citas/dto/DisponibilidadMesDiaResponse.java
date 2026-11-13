package es.hair.backend.citas.dto;

import java.time.LocalDate;

public record DisponibilidadMesDiaResponse(
        LocalDate fecha,
        EstadoDisponibilidad estado,
        MotivoCierre motivoCierre,
        int huecosDisponibles,
        int totalHuecos
) {
}
