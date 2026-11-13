package es.hair.backend.citas.service;

import es.hair.backend.citas.dao.CitaDao;
import es.hair.backend.citas.dto.CrearCitaAdminRequest;
import es.hair.backend.citas.integration.DisponibilidadConsulta;
import es.hair.backend.citas.integration.UsuarioConsulta;
import es.hair.backend.citas.integration.UsuarioConsulta.DatosClienteCita;
import es.hair.backend.citas.integration.UsuarioConsulta.UsuarioActivoParaCita;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(CitaServiceRollbackIntegrationTest.DependenciasDePrueba.class)
class CitaServiceRollbackIntegrationTest {

    @Autowired
    private CitaService citaService;

    @Autowired
    private CitaDao citaDao;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EstadoPrueba estadoPrueba;

    @Test
    void revierteElInsertTrasUnFalloPosteriorAInsertarSinTransaccionExterna() throws SQLException {
        assertTrue(AopUtils.isAopProxy(citaService),
                "La llamada debe atravesar el proxy transaccional de Spring");

        Integer usuarioId = crearUsuario();
        LocalDate fecha = LocalDate.now(ZoneId.of("Europe/Madrid")).plusDays(30);
        LocalDateTime inicio = fecha.atTime(LocalTime.of(10, 0));
        estadoPrueba.configurar(usuarioId, inicio);

        try {
            IllegalStateException fallo = assertThrows(IllegalStateException.class,
                    () -> citaService.crearParaAdministrador(
                            new CrearCitaAdminRequest(usuarioId, fecha, inicio.toLocalTime())));

            assertEquals("Fallo intencional durante el enriquecimiento posterior al INSERT",
                    fallo.getMessage());
            assertTrue(estadoPrueba.insertDetectado(),
                    "El DAO debe haber leído la cita recién insertada dentro de la transacción");
            assertTrue(estadoPrueba.transaccionDetectada(),
                    "El proveedor debe ejecutarse dentro de la transacción del proxy");
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                    "La transacción del servicio debe haber terminado antes de verificar el rollback");

            assertEquals(0, contarCitasEnConexionNueva(usuarioId, inicio),
                    "La conexión independiente no debe observar la cita revertida");
        } finally {
            limpiarUsuarioDePrueba(usuarioId);
        }
    }

    private Integer crearUsuario() throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO usuario (nombre, apellidos, email, google_sub)
                VALUES (?, ?, ?, ?)
                """, PreparedStatement.RETURN_GENERATED_KEYS)) {
            String unique = java.util.UUID.randomUUID().toString();
            statement.setString(1, "Rollback Test");
            statement.setString(2, "Usuario");
            statement.setString(3, "cita-rollback-" + unique + "@example.invalid");
            statement.setString(4, "cita-rollback-" + unique);
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

    private int contarCitasEnConexionNueva(Integer usuarioId, LocalDateTime inicio)
            throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM cita
                     WHERE usuario_id = ? AND inicio = ?
                     """)) {
            statement.setInt(1, usuarioId);
            statement.setObject(2, inicio);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("La consulta de verificación no devolvió resultados");
                }
                return resultSet.getInt(1);
            }
        }
    }

    private void limpiarUsuarioDePrueba(Integer usuarioId) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement borrarCitas = connection.prepareStatement(
                    "DELETE FROM cita WHERE usuario_id = ?")) {
                borrarCitas.setInt(1, usuarioId);
                borrarCitas.executeUpdate();
            }
            try (PreparedStatement borrarUsuario = connection.prepareStatement(
                    "DELETE FROM usuario WHERE id = ?")) {
                borrarUsuario.setInt(1, usuarioId);
                borrarUsuario.executeUpdate();
            }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DependenciasDePrueba {

        @Bean
        EstadoPrueba estadoPrueba() {
            return new EstadoPrueba();
        }

        @Bean
        DisponibilidadConsulta disponibilidadConsulta() {
            return inicio -> true;
        }

        @Bean
        UsuarioConsulta usuarioConsulta(CitaDao citaDao, EstadoPrueba estadoPrueba) {
            return new UsuarioConsulta() {
                @Override
                public Optional<UsuarioActivoParaCita> obtenerUsuarioActivoParaCita(
                        Integer usuarioId) {
                    return Optional.of(new UsuarioActivoParaCita(usuarioId));
                }

                @Override
                public Map<Integer, DatosClienteCita> obtenerDatosClientesPorIds(
                        Set<Integer> usuarioIds) {
                    Integer usuarioId = estadoPrueba.usuarioId();
                    LocalDateTime inicio = estadoPrueba.inicio();
                    boolean detectada = citaDao.buscarPorUsuario(usuarioId).stream()
                            .anyMatch(cita -> cita.inicio().equals(inicio));
                    estadoPrueba.marcarInsercionDetectada(detectada);
                    estadoPrueba.marcarTransaccionDetectada(
                            TransactionSynchronizationManager.isActualTransactionActive());
                    throw new IllegalStateException(
                            "Fallo intencional durante el enriquecimiento posterior al INSERT");
                }
            };
        }
    }

    static class EstadoPrueba {

        private Integer usuarioId;
        private LocalDateTime inicio;
        private boolean insertDetectado;
        private boolean transaccionDetectada;

        void configurar(Integer usuarioId, LocalDateTime inicio) {
            this.usuarioId = usuarioId;
            this.inicio = inicio;
        }

        Integer usuarioId() {
            return usuarioId;
        }

        LocalDateTime inicio() {
            return inicio;
        }

        boolean insertDetectado() {
            return insertDetectado;
        }

        boolean transaccionDetectada() {
            return transaccionDetectada;
        }

        void marcarInsercionDetectada(boolean detectada) {
            insertDetectado = detectada;
        }

        void marcarTransaccionDetectada(boolean detectada) {
            transaccionDetectada = detectada;
        }
    }
}
