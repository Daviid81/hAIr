package es.hair.backend.citas.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorarioPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(HorarioTestConfiguration.class);

    @Test
    void generaExactamenteLosOnceHuecosAprobados() {
        HorarioProperties horario = new HorarioProperties();

        assertTrue(horario.isHorarioValido());
        assertEquals(List.of(
                LocalTime.of(10, 0),
                LocalTime.of(10, 40),
                LocalTime.of(11, 20),
                LocalTime.of(12, 0),
                LocalTime.of(12, 40),
                LocalTime.of(15, 30),
                LocalTime.of(16, 10),
                LocalTime.of(16, 50),
                LocalTime.of(17, 30),
                LocalTime.of(18, 10),
                LocalTime.of(18, 50)
        ), horario.generarHuecosDiarios());
    }

    @Test
    void invalidaLosLimitesAunqueSeMantengaElMismoNumeroDeHuecos() {
        HorarioProperties horario = new HorarioProperties();
        horario.setInicioManana(LocalTime.of(10, 40));
        horario.setFinManana(LocalTime.of(14, 0));

        assertEquals(11, horario.generarHuecosDiarios().size());
        assertTrue(!horario.isHorarioValido());
    }

    @Test
    void springCargaLasPropiedadesValidasYPermiteIniciarElContexto() {
        contextRunner
                .withPropertyValues(
                        "app.citas.horario.inicio-manana=10:00",
                        "app.citas.horario.fin-manana=13:20",
                        "app.citas.horario.inicio-tarde=15:30",
                        "app.citas.horario.fin-tarde=19:30",
                        "app.citas.horario.zona-horaria=Europe/Madrid"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    HorarioProperties horario = context.getBean(HorarioProperties.class);
                    assertEquals(LocalTime.of(10, 0), horario.getInicioManana());
                    assertEquals(LocalTime.of(13, 20), horario.getFinManana());
                    assertEquals(LocalTime.of(15, 30), horario.getInicioTarde());
                    assertEquals(LocalTime.of(19, 30), horario.getFinTarde());
                    assertEquals("Europe/Madrid", horario.getZonaHoraria().getId());
                });
    }

    @Test
    void springImpideIniciarElContextoConUnHorarioDistintoAlAprobado() {
        contextRunner
                .withPropertyValues(
                        "app.citas.horario.inicio-manana=10:40",
                        "app.citas.horario.fin-manana=14:00",
                        "app.citas.horario.inicio-tarde=15:30",
                        "app.citas.horario.fin-tarde=19:30",
                        "app.citas.horario.zona-horaria=Europe/Madrid"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("HorarioProperties");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(HorarioProperties.class)
    static class HorarioTestConfiguration {
    }
}
