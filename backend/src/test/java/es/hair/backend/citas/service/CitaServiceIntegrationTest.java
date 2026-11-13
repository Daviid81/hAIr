package es.hair.backend.citas.service;

import es.hair.backend.citas.dao.CitaDao;
import es.hair.backend.citas.dto.CitaResponse;
import es.hair.backend.citas.dto.CrearCitaRequest;
import es.hair.backend.citas.integration.DisponibilidadConsulta;
import es.hair.backend.citas.integration.UsuarioConsulta;
import es.hair.backend.citas.integration.UsuarioConsulta.DatosClienteCita;
import es.hair.backend.citas.integration.UsuarioConsulta.UsuarioActivoParaCita;
import es.hair.backend.citas.model.Cita;
import es.hair.backend.citas.model.EstadoCita;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(CitaServiceIntegrationTest.DependenciasDePrueba.class)
class CitaServiceIntegrationTest {

    @Autowired
    private CitaService citaService;

    @Autowired
    private CitaDao citaDao;

    @Autowired
    private DataSource dataSource;

    @Test
    @Transactional
    void creaYCancelaCitaConDaoYMySqlRealesDentroDeLaTransaccion() throws SQLException {
        Integer usuarioId = crearUsuario();
        LocalDateTime inicio = LocalDate.now().plusDays(30).atTime(10, 0);

        CitaResponse creada = citaService.crearParaCliente(
                usuarioId, new CrearCitaRequest(inicio.toLocalDate(), inicio.toLocalTime()));

        Cita persistida = citaDao.buscarPorId(creada.id()).orElseThrow();
        assertEquals(EstadoCita.CONFIRMADA, persistida.estado());
        assertEquals(inicio.plusMinutes(40), persistida.fin());

        CitaResponse cancelada = citaService.cancelarPorCliente(usuarioId, creada.id());

        assertEquals(EstadoCita.CANCELADA, cancelada.estado());
        assertEquals(EstadoCita.CANCELADA,
                citaDao.buscarPorId(creada.id()).orElseThrow().estado());
    }

    @Test
    @Transactional
    void cancelacionPorBloqueoParticipaEnLaTransaccionActual() throws SQLException {
        Integer usuarioId = crearUsuario();
        LocalDateTime inicio = LocalDate.now().plusDays(30).atTime(10, 0);
        CitaResponse creada = citaService.crearParaCliente(
                usuarioId, new CrearCitaRequest(inicio.toLocalDate(), inicio.toLocalTime()));

        var canceladas = citaService.cancelarPorBloqueo(
                inicio.minusMinutes(1), inicio.plusMinutes(40));

        assertEquals(1, canceladas.size());
        assertEquals(creada.id(), canceladas.getFirst().citaId());
        assertEquals(EstadoCita.CANCELADA,
                citaDao.buscarPorId(creada.id()).orElseThrow().estado());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cancelacionPorBloqueoExigeUnaTransaccionIniciadaPorElLlamador() {
        LocalDateTime inicio = LocalDate.now().plusDays(30).atTime(10, 0);

        assertThrows(IllegalTransactionStateException.class,
                () -> citaService.cancelarPorBloqueo(inicio, inicio.plusMinutes(40)));
    }

    private Integer crearUsuario() throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO usuario (nombre, apellidos, email, google_sub)
                VALUES (?, ?, ?, ?)
                """, PreparedStatement.RETURN_GENERATED_KEYS)) {
            String unique = java.util.UUID.randomUUID().toString();
            statement.setString(1, "Service Test");
            statement.setString(2, "Usuario");
            statement.setString(3, "cita-service-" + unique + "@example.invalid");
            statement.setString(4, "cita-service-" + unique);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("La creación del usuario de prueba no devolvió ID");
                }
                return generatedKeys.getInt(1);
            }
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DependenciasDePrueba {

        @Bean
        DisponibilidadConsulta disponibilidadConsulta() {
            return inicio -> true;
        }

        @Bean
        UsuarioConsulta usuarioConsulta() {
            return new UsuarioConsulta() {
                @Override
                public Optional<UsuarioActivoParaCita> obtenerUsuarioActivoParaCita(
                        Integer usuarioId) {
                    return Optional.of(new UsuarioActivoParaCita(usuarioId));
                }

                @Override
                public Map<Integer, DatosClienteCita> obtenerDatosClientesPorIds(
                        Set<Integer> usuarioIds) {
                    return usuarioIds.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                            id -> id,
                            id -> new DatosClienteCita(id, "Service", "Test", null)));
                }
            };
        }
    }
}
