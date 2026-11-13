package es.hair.backend.citas.exception;

public class CitaNoCancelableException extends RuntimeException {

    public CitaNoCancelableException(String mensaje) {
        super(mensaje);
    }
}
