package es.hair.backend.ai.stub;

import es.hair.backend.ai.port.DisponibilidadPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** DATOS DE PRUEBA. Solo con el perfil "ia-stub". Se elimina cuando exista el servicio real. */
@Component
@Profile("ia-stub")
public class StubDisponibilidad implements DisponibilidadPort {

    private static final int[][] FRANJAS = {{600, 800}, {930, 1170}};   // 10:00-13:20 y 15:30-19:30

    @Override
    public List<LocalDateTime> huecosLibres(LocalDate fecha) {
        List<LocalDateTime> libres = new ArrayList<>();
        if (fecha.getDayOfWeek() == DayOfWeek.SATURDAY || fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return libres;   // fines de semana cerrados por defecto
        }
        for (int[] franja : FRANJAS) {
            for (int t = franja[0]; t + 40 <= franja[1]; t += 40) {
                LocalDateTime hueco = fecha.atTime(t / 60, t % 60);
                boolean ocupado = (t / 40) % 3 == 1;   // simula citas de otros clientes
                if (!ocupado && hueco.isAfter(LocalDateTime.now())) {
                    libres.add(hueco);
                }
            }
        }
        return libres;
    }
}
