package es.hair.backend.ai.port;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Contrato con la persona que implementa la disponibilidad (Persona 2). */
public interface DisponibilidadPort {

    /** Huecos libres de 40 min de un día. Lista vacía si está cerrado, completo o bloqueado. */
    List<LocalDateTime> huecosLibres(LocalDate fecha);
}
