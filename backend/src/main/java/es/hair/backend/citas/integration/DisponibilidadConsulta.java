package es.hair.backend.citas.integration;

import java.time.LocalDateTime;

/**
 * Contrato para que CitaService delegue la validación del hueco al módulo
 * de disponibilidad, que solo devuelve true para un inicio permitido que no
 * esté bloqueado ni solapado con otra cita CONFIRMADA.
 */
@FunctionalInterface
public interface DisponibilidadConsulta {

    boolean estaDisponible(LocalDateTime inicio);
}
