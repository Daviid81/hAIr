package es.hair.backend.citas.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.citas.horario")
public class HorarioProperties {

    private static final int DURACION_CITA_MINUTOS = 40;
    private static final int TOTAL_HUECOS_DIARIOS = 11;

    @NotNull
    private LocalTime inicioManana = LocalTime.of(10, 0);

    @NotNull
    private LocalTime finManana = LocalTime.of(13, 20);

    @NotNull
    private LocalTime inicioTarde = LocalTime.of(15, 30);

    @NotNull
    private LocalTime finTarde = LocalTime.of(19, 30);

    @NotNull
    private ZoneId zonaHoraria = ZoneId.of("Europe/Madrid");

    public LocalTime getInicioManana() {
        return inicioManana;
    }

    public void setInicioManana(LocalTime inicioManana) {
        this.inicioManana = inicioManana;
    }

    public LocalTime getFinManana() {
        return finManana;
    }

    public void setFinManana(LocalTime finManana) {
        this.finManana = finManana;
    }

    public LocalTime getInicioTarde() {
        return inicioTarde;
    }

    public void setInicioTarde(LocalTime inicioTarde) {
        this.inicioTarde = inicioTarde;
    }

    public LocalTime getFinTarde() {
        return finTarde;
    }

    public void setFinTarde(LocalTime finTarde) {
        this.finTarde = finTarde;
    }

    public ZoneId getZonaHoraria() {
        return zonaHoraria;
    }

    public void setZonaHoraria(ZoneId zonaHoraria) {
        this.zonaHoraria = zonaHoraria;
    }

    public List<LocalTime> generarHuecosDiarios() {
        List<LocalTime> huecos = new ArrayList<>();
        agregarHuecos(huecos, inicioManana, finManana);
        agregarHuecos(huecos, inicioTarde, finTarde);
        return List.copyOf(huecos);
    }

    @AssertTrue(message = "El horario debe coincidir con los límites y la zona horaria aprobados")
    public boolean isHorarioValido() {
        if (inicioManana == null || finManana == null || inicioTarde == null || finTarde == null
                || zonaHoraria == null) {
            return false;
        }

        return inicioManana.equals(LocalTime.of(10, 0))
                && finManana.equals(LocalTime.of(13, 20))
                && inicioTarde.equals(LocalTime.of(15, 30))
                && finTarde.equals(LocalTime.of(19, 30))
                && zonaHoraria.equals(ZoneId.of("Europe/Madrid"))
                && generarHuecosDiarios().size() == TOTAL_HUECOS_DIARIOS;
    }

    private void agregarHuecos(List<LocalTime> huecos, LocalTime inicio, LocalTime fin) {
        if (inicio == null || fin == null || !inicio.isBefore(fin)) {
            return;
        }

        LocalTime hueco = inicio;
        while (!hueco.plusMinutes(DURACION_CITA_MINUTOS).isAfter(fin)) {
            huecos.add(hueco);
            hueco = hueco.plusMinutes(DURACION_CITA_MINUTOS);
        }
    }
}
