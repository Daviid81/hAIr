package es.hair.backend.exception;

public class ArchivoInvalidoException extends RuntimeException {      // 400
    public ArchivoInvalidoException(String mensaje) { super(mensaje); }
}