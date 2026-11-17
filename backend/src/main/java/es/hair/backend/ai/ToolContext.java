package es.hair.backend.ai;

import es.hair.backend.ai.dto.ConfirmacionDto;

import java.util.ArrayList;
import java.util.List;

/** Estado de un turno de conversación: quién es el usuario (viene del JWT) y qué datos reales se han consultado. */
public class ToolContext {

    private final long usuarioId;
    private final List<String> huecos = new ArrayList<>();
    private ConfirmacionDto confirmacion;

    public ToolContext(long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public long usuarioId() {
        return usuarioId;
    }

    public List<String> huecos() {
        return huecos;
    }

    public ConfirmacionDto confirmacion() {
        return confirmacion;
    }

    public void setConfirmacion(ConfirmacionDto confirmacion) {
        this.confirmacion = confirmacion;
    }
}
