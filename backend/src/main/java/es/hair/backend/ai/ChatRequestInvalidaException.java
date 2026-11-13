package es.hair.backend.ai;

/** La petición del chat no cumple las reglas de entrada. Se traduce a HTTP 400. */
public class ChatRequestInvalidaException extends RuntimeException {

    public ChatRequestInvalidaException(String message) {
        super(message);
    }
}
