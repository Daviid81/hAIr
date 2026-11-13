package es.hair.backend.exception;

public class ErrorPersistenciaException extends RuntimeException { // 500
    public ErrorPersistenciaException(String message) {
        super(message);
    }

    public ErrorPersistenciaException(String message, Throwable cause) {
        super(message, cause);
    }
}
