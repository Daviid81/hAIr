package es.hair.backend.ai.stub;

import es.hair.backend.ai.port.CitaResumen;
import es.hair.backend.ai.port.CitasPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** DATOS DE PRUEBA. Solo con el perfil "ia-stub". Se elimina cuando exista el servicio real. */
@Component
@Profile("ia-stub")
public class StubCitas implements CitasPort {

    @Override
    public List<CitaResumen> citasProximas(long usuarioId) {
        LocalDateTime inicio = LocalDate.now().plusDays(6).atTime(10, 40);
        LocalDateTime limite = inicio.toLocalDate().minusDays(2).atTime(23, 59);
        return List.of(new CitaResumen(1, inicio, "CONFIRMADA", LocalDateTime.now().isBefore(limite), limite));
    }
}
