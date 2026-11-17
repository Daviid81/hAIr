package es.hair.backend.auth.exception;

public class GoogleTokenInvalidoException extends RuntimeException {

    public GoogleTokenInvalidoException() {
        super("El token de Google no es válido");
    }

    public GoogleTokenInvalidoException(Throwable cause) {
        super("No se pudo verificar el token de Google", cause);
    }
}
