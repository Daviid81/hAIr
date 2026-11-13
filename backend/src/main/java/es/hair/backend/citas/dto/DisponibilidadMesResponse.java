package es.hair.backend.citas.dto;

import java.util.List;

public record DisponibilidadMesResponse(
        int mes,
        int anio,
        List<DisponibilidadMesDiaResponse> dias
) {
}
