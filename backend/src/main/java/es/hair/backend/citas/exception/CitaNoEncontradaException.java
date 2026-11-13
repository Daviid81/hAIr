package es.hair.backend.citas.exception;

public class CitaNoEncontradaException extends RuntimeException {

    public CitaNoEncontradaException(Integer citaId) {
        super("No se encontró la cita con ID " + citaId);
    }
}
