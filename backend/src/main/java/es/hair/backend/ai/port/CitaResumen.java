package es.hair.backend.ai.port;

import java.time.LocalDateTime;

public record CitaResumen(int id, LocalDateTime inicio, String estado,
                          boolean cancelable, LocalDateTime limiteCancelacion) {
}
