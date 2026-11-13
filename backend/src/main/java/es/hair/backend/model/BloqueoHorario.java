package es.hair.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record BloqueoHorario(Long id, LocalDate fecha, LocalTime HoraInicio, LocalTime HoraFin, String motivo, LocalDateTime createdAt) {
}
