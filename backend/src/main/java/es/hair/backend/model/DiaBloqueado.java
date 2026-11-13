package es.hair.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiaBloqueado(Long id, LocalDate fecha, String motivo, Long archivoId, LocalDateTime createdAt) {

}
