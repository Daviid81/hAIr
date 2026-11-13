package es.hair.backend.citas.exception;

public class CitaAjenaException extends RuntimeException {

    public CitaAjenaException() {
        super("La cita no pertenece al cliente autenticado");
    }
}
