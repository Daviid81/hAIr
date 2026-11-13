package es.hair.backend.exception;

public class ArchivoDemasiadoGrandeException extends RuntimeException { // 413
    public ArchivoDemasiadoGrandeException(String message) {
        super(message);
    }
}
