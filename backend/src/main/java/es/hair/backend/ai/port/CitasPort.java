package es.hair.backend.ai.port;

import java.util.List;

/** Contrato con la persona que implementa las citas (Persona 2). */
public interface CitasPort {

    /** Citas futuras CONFIRMADAS del usuario. */
    List<CitaResumen> citasProximas(long usuarioId);
}
