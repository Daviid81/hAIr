package es.hair.backend.citas.exception;

public class CitaFuturaExistenteException extends RuntimeException {

    public CitaFuturaExistenteException(Integer usuarioId) {
        super("El usuario " + usuarioId + " ya tiene una cita futura confirmada");
    }
}
