package es.hair.backend.exception;

public class FormatoNoSoportadoException extends RuntimeException { // 415
    public FormatoNoSoportadoException(String message) {
        super(message);
    }
}
