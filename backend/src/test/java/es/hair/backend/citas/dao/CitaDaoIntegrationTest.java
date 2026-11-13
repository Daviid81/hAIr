package es.hair.backend.citas.dao;

import es.hair.backend.citas.model.Cita;
import es.hair.backend.citas.model.EstadoCita;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class CitaDaoIntegrationTest {

    @Autowired
    private CitaDao citaDao;

    @Autowired
    private DataSource dataSource;

    private Integer usuarioId;
    private Integer otroUsuarioId;
    private LocalDateTime horaBase;

    @BeforeEach
    void prepararUsuarios() throws SQLException {
        usuarioId = crearUsuario();
        otroUsuarioId = crearUsuario();
        horaBase = LocalDate.now().plusDays(20).atTime(10, 0).withNano(0);
    }

    @Test
    void insertaYRecuperaUnaCita() {
        Integer citaId = insertarCita(usuarioId, horaBase, EstadoCita.CONFIRMADA);

        Cita cita = citaDao.buscarPorId(citaId).orElseThrow();

        assertEquals(citaId, cita.id());
        assertEquals(usuarioId, cita.usuarioId());
        assertEquals(horaBase, cita.inicio());
        assertEquals(horaBase.plusMinutes(40), cita.fin());
        assertEquals(EstadoCita.CONFIRMADA, cita.estado());
        assertNotNull(cita.createdAt());
        assertNotNull(cita.updatedAt());
    }

    @Test
    void filtraCitasPorUsuarioYDetectaSoloSusCitasFuturasConfirmadas() {
        Integer propia = insertarCita(usuarioId, horaBase, EstadoCita.CONFIRMADA);
        insertarCita(otroUsuarioId, horaBase.plusHours(1), EstadoCita.CONFIRMADA);

        List<Cita> citas = citaDao.buscarPorUsuario(usuarioId);

        assertEquals(1, citas.size());
        assertEquals(propia, citas.getFirst().id());
        assertTrue(citaDao.existeCitaFuturaConfirmada(usuarioId, horaBase.minusMinutes(1), null));
        assertTrue(citaDao.existeCitaFuturaConfirmada(
                otroUsuarioId, horaBase.minusMinutes(1), null));
        assertFalse(citaDao.existeCitaFuturaConfirmada(
                usuarioId, horaBase.minusMinutes(1), propia));
    }

    @Test
    void unaCitaCanceladaNoCuentaComoCitaFuturaConfirmada() {
        Integer citaId = insertarCita(usuarioId, horaBase, EstadoCita.CONFIRMADA);
        LocalDateTime referencia = horaBase.minusMinutes(1);

        assertTrue(citaDao.existeCitaFuturaConfirmada(usuarioId, referencia, null));
        assertTrue(citaDao.cancelarConfirmadaPorId(citaId));

        assertFalse(citaDao.existeCitaFuturaConfirmada(usuarioId, referencia, null));
    }

    @Test
    void filtraPorEstadoYFecha() {
        Integer confirmada = insertarCita(usuarioId, horaBase, EstadoCita.CONFIRMADA);
        Integer cancelada = insertarCita(
                usuarioId, horaBase.plusHours(1), EstadoCita.CANCELADA);
        insertarCita(usuarioId, horaBase.plusDays(1), EstadoCita.CONFIRMADA);
        LocalDate fecha = horaBase.toLocalDate();

        assertEquals(List.of(confirmada), citaDao.buscarPorFechaYEstado(
                fecha, EstadoCita.CONFIRMADA).stream().map(Cita::id).toList());
        assertEquals(List.of(cancelada), citaDao.buscarPorFechaYEstado(
                fecha, EstadoCita.CANCELADA).stream().map(Cita::id).toList());
        assertEquals(2, citaDao.buscarPorFechaYEstado(fecha, null).size());
    }

    @Test
    void buscaSolapamientosConIntervalosSemiabiertosYExcluyeCanceladas() {
        Integer solapada = insertarCita(
                usuarioId, horaBase.minusMinutes(20), EstadoCita.CONFIRMADA);
        insertarCita(usuarioId, horaBase, EstadoCita.CANCELADA);
        insertarCita(usuarioId, horaBase.plusHours(1), EstadoCita.CONFIRMADA);
        insertarCita(
                usuarioId, horaBase.minusMinutes(40), EstadoCita.CONFIRMADA);

        List<Cita> solapamientos = citaDao.buscarSolapamientosConfirmados(
                horaBase, horaBase.plusHours(1));

        assertEquals(List.of(solapada), solapamientos.stream().map(Cita::id).toList());
    }

    @Test
    void cancelaCondicionalmenteSoloUnaCitaConfirmada() {
        Integer citaId = insertarCita(usuarioId, horaBase, EstadoCita.CONFIRMADA);
        Integer canceladaId = insertarCita(
                usuarioId, horaBase.plusHours(1), EstadoCita.CANCELADA);

        assertTrue(citaDao.cancelarConfirmadaPorId(citaId));
        assertFalse(citaDao.cancelarConfirmadaPorId(citaId));
        assertFalse(citaDao.cancelarConfirmadaPorId(canceladaId));
        assertEquals(EstadoCita.CANCELADA,
                citaDao.buscarPorId(citaId).orElseThrow().estado());
    }

    @Test
    void cancelaSoloConfirmadasSolapadasConElIntervalo() {
        Integer solapada = insertarCita(
                usuarioId, horaBase.minusMinutes(20), EstadoCita.CONFIRMADA);
        Integer canceladaSolapada = insertarCita(
                usuarioId, horaBase, EstadoCita.CANCELADA);
        Integer terminaEnElInicio = insertarCita(
                usuarioId, horaBase.minusMinutes(40), EstadoCita.CONFIRMADA);
        Integer empiezaEnElFin = insertarCita(
                usuarioId, horaBase.plusHours(1), EstadoCita.CONFIRMADA);

        int afectadas = citaDao.cancelarConfirmadasSolapadasEnIntervalo(
                horaBase, horaBase.plusHours(1));

        assertEquals(1, afectadas);
        assertEquals(EstadoCita.CANCELADA,
                citaDao.buscarPorId(solapada).orElseThrow().estado());
        assertEquals(EstadoCita.CANCELADA,
                citaDao.buscarPorId(canceladaSolapada).orElseThrow().estado());
        assertEquals(EstadoCita.CONFIRMADA,
                citaDao.buscarPorId(terminaEnElInicio).orElseThrow().estado());
        assertEquals(EstadoCita.CONFIRMADA,
                citaDao.buscarPorId(empiezaEnElFin).orElseThrow().estado());
    }

    private Integer insertarCita(Integer usuario, LocalDateTime inicio, EstadoCita estado) {
        return citaDao.insertar(new Cita(
                null,
                usuario,
                inicio,
                inicio.plusMinutes(40),
                estado,
                null,
                null
        ));
    }

    private Integer crearUsuario() throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO usuario (nombre, apellidos, email, google_sub)
                VALUES (?, ?, ?, ?)
                """, PreparedStatement.RETURN_GENERATED_KEYS)) {
            String unique = java.util.UUID.randomUUID().toString();
            statement.setString(1, "DAO Test");
            statement.setString(2, "Usuario");
            statement.setString(3, "cita-dao-" + unique + "@example.invalid");
            statement.setString(4, "cita-dao-" + unique);
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
}
