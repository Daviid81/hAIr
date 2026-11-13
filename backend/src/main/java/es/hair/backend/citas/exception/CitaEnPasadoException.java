package es.hair.backend.citas.exception;

import java.time.LocalDateTime;

public class CitaEnPasadoException extends RuntimeException {

    public CitaEnPasadoException(LocalDateTime inicio) {
        super("No se puede reservar una cita en el pasado: " + inicio);
    }
}
