package es.hair.backend.citas.exception;

public class UsuarioDestinoNoValidoException extends RuntimeException {

    public UsuarioDestinoNoValidoException(Integer usuarioId) {
        super("El usuario destinatario no existe o no está activo: " + usuarioId);
    }
}
