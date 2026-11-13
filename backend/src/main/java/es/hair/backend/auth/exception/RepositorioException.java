package es.hair.backend.auth.exception;

public class RepositorioException extends RuntimeException {

    public RepositorioException(String message) {
        super(message);
    }

    public RepositorioException(String message, Throwable cause) {
        super(message, cause);
    }
}
