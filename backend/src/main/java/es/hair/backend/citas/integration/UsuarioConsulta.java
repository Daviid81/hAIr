package es.hair.backend.citas.integration;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface UsuarioConsulta {

    Optional<UsuarioActivoParaCita> obtenerUsuarioActivoParaCita(Integer usuarioId);

    Map<Integer, DatosClienteCita> obtenerDatosClientesPorIds(Set<Integer> usuarioIds);

    record UsuarioActivoParaCita(Integer id) {
    }

    record DatosClienteCita(
            Integer id,
            String nombre,
            String apellidos,
            String telefono
    ) {
    }
}
