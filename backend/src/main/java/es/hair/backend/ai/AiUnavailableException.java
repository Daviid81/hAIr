package es.hair.backend.ai;

/** La IA no está disponible (límite de uso, error de red o respuesta inválida). Se traduce a HTTP 503. */
public class AiUnavailableException extends RuntimeException {

    private final int retryAfterSeconds;

    public AiUnavailableException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
