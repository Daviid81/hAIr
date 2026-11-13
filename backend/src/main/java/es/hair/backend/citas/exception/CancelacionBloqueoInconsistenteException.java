package es.hair.backend.citas.exception;

public class CancelacionBloqueoInconsistenteException extends RuntimeException {

    public CancelacionBloqueoInconsistenteException(int encontradas, int actualizadas) {
        super("La cancelación por intervalo encontró " + encontradas
                + " citas y actualizó " + actualizadas);
    }
}
