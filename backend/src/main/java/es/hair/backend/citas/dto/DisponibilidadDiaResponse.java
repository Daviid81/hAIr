package es.hair.backend.citas.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DisponibilidadDiaResponse(
        LocalDate fecha,
        EstadoDisponibilidad estado,
        MotivoCierre motivoCierre,
        List<LocalTime> huecosDisponibles,
        int totalHuecos
) {
}
