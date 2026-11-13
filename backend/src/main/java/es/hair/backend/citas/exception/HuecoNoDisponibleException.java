package es.hair.backend.citas.exception;

import java.time.LocalDateTime;

public class HuecoNoDisponibleException extends RuntimeException {

    public HuecoNoDisponibleException(LocalDateTime inicio) {
        super("El hueco solicitado no está disponible: " + inicio);
    }
}
