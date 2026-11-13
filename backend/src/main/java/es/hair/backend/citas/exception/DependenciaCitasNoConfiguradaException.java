package es.hair.backend.citas.exception;

public class DependenciaCitasNoConfiguradaException extends RuntimeException {

    public DependenciaCitasNoConfiguradaException(String dependencia) {
        super("La dependencia requerida no está configurada: " + dependencia);
    }
}
