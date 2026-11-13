package es.hair.backend.citas.dao;

import es.hair.backend.citas.model.Cita;
import es.hair.backend.citas.model.EstadoCita;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class CitaDao {

    private static final String COLUMNAS_CITA =
            "id, usuario_id, inicio, fin, estado, created_at, updated_at";

    private static final String INSERTAR_CITA = """
            INSERT INTO cita (usuario_id, inicio, fin, estado)
            VALUES (?, ?, ?, ?)
            """;

    private static final String BUSCAR_POR_ID = """
            SELECT %s
            FROM cita
            WHERE id = ?
            """.formatted(COLUMNAS_CITA);

    private static final String BUSCAR_POR_USUARIO = """
            SELECT %s
            FROM cita
            WHERE usuario_id = ?
            ORDER BY inicio, id
            """.formatted(COLUMNAS_CITA);

    private static final String EXISTE_FUTURA_CONFIRMADA = """
            SELECT 1
            FROM cita
            WHERE usuario_id = ?
              AND estado = 'CONFIRMADA'
              AND inicio > ?
            LIMIT 1
            """;

    private static final String EXISTE_OTRA_FUTURA_CONFIRMADA = """
            SELECT 1
            FROM cita
            WHERE usuario_id = ?
              AND estado = 'CONFIRMADA'
              AND inicio > ?
              AND id <> ?
            LIMIT 1
            """;

    private static final String BUSCAR_SOLAPAMIENTOS_CONFIRMADOS = """
            SELECT %s
            FROM cita
            WHERE estado = 'CONFIRMADA'
              AND inicio < ?
              AND fin > ?
            ORDER BY inicio, id
            """.formatted(COLUMNAS_CITA);

    private static final String BUSCAR_POR_FECHA = """
            SELECT %s
            FROM cita
            WHERE inicio >= ?
              AND inicio < ?
            ORDER BY inicio, id
            """.formatted(COLUMNAS_CITA);

    private static final String BUSCAR_POR_FECHA_Y_ESTADO = """
            SELECT %s
            FROM cita
            WHERE inicio >= ?
              AND inicio < ?
              AND estado = ?
            ORDER BY inicio, id
            """.formatted(COLUMNAS_CITA);

    private static final String CANCELAR_POR_ID = """
            UPDATE cita
            SET estado = 'CANCELADA'
            WHERE id = ?
              AND estado = 'CONFIRMADA'
            """;

    private static final String CANCELAR_SOLAPAMIENTOS_CONFIRMADOS = """
            UPDATE cita
            SET estado = 'CANCELADA'
            WHERE estado = 'CONFIRMADA'
              AND inicio < ?
              AND fin > ?
            """;

    private final DataSource dataSource;

    public CitaDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Integer insertar(Cita cita) {
        Objects.requireNonNull(cita, "La cita es obligatoria");
        Objects.requireNonNull(cita.usuarioId(), "El usuario de la cita es obligatorio");
        Objects.requireNonNull(cita.inicio(), "El inicio de la cita es obligatorio");
        Objects.requireNonNull(cita.fin(), "El fin de la cita es obligatorio");
        Objects.requireNonNull(cita.estado(), "El estado de la cita es obligatorio");

        return ejecutar("Insertar cita", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    INSERTAR_CITA, PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, cita.usuarioId());
                statement.setObject(2, cita.inicio());
                statement.setObject(3, cita.fin());
                statement.setString(4, cita.estado().name());
                statement.executeUpdate();

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        throw new SQLException("La inserción no devolvió el ID generado");
                    }
                    return generatedKeys.getInt(1);
                }
            }
        });
    }

    public Optional<Cita> buscarPorId(Integer citaId) {
        Objects.requireNonNull(citaId, "El ID de la cita es obligatorio");

        return ejecutar("Buscar cita por ID", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(BUSCAR_POR_ID)) {
                statement.setInt(1, citaId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next()
                            ? Optional.of(mapearCita(resultSet))
                            : Optional.empty();
                }
            }
        });
    }

    public List<Cita> buscarPorUsuario(Integer usuarioId) {
        Objects.requireNonNull(usuarioId, "El ID del usuario es obligatorio");

        return ejecutar("Buscar citas por usuario", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(BUSCAR_POR_USUARIO)) {
                statement.setInt(1, usuarioId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return mapearCitas(resultSet);
                }
            }
        });
    }

    public boolean existeCitaFuturaConfirmada(Integer usuarioId, LocalDateTime desde,
                                               Integer excluirCitaId) {
        Objects.requireNonNull(usuarioId, "El ID del usuario es obligatorio");
        Objects.requireNonNull(desde, "La fecha de referencia es obligatoria");

        String sql = excluirCitaId == null
                ? EXISTE_FUTURA_CONFIRMADA
                : EXISTE_OTRA_FUTURA_CONFIRMADA;

        return ejecutar("Comprobar citas futuras confirmadas", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, usuarioId);
                statement.setObject(2, desde);
                if (excluirCitaId != null) {
                    statement.setInt(3, excluirCitaId);
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        });
    }

    public List<Cita> buscarSolapamientosConfirmados(LocalDateTime inicio, LocalDateTime fin) {
        Objects.requireNonNull(inicio, "El inicio del intervalo es obligatorio");
        Objects.requireNonNull(fin, "El fin del intervalo es obligatorio");

        return ejecutar("Buscar citas confirmadas solapadas", connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement(BUSCAR_SOLAPAMIENTOS_CONFIRMADOS)) {
                statement.setObject(1, fin);
                statement.setObject(2, inicio);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return mapearCitas(resultSet);
                }
            }
        });
    }

    public List<Cita> buscarPorFechaYEstado(LocalDate fecha, EstadoCita estado) {
        Objects.requireNonNull(fecha, "La fecha es obligatoria");
        String sql = estado == null ? BUSCAR_POR_FECHA : BUSCAR_POR_FECHA_Y_ESTADO;
        LocalDateTime inicioDelDia = fecha.atStartOfDay();
        LocalDateTime inicioDiaSiguiente = fecha.plusDays(1).atStartOfDay();

        return ejecutar("Buscar citas por fecha y estado", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setObject(1, inicioDelDia);
                statement.setObject(2, inicioDiaSiguiente);
                if (estado != null) {
                    statement.setString(3, estado.name());
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    return mapearCitas(resultSet);
                }
            }
        });
    }

    public boolean cancelarConfirmadaPorId(Integer citaId) {
        Objects.requireNonNull(citaId, "El ID de la cita es obligatorio");

        return ejecutar("Cancelar cita confirmada por ID", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(CANCELAR_POR_ID)) {
                statement.setInt(1, citaId);
                return statement.executeUpdate() == 1;
            }
        });
    }

    public int cancelarConfirmadasSolapadasEnIntervalo(LocalDateTime inicio, LocalDateTime fin) {
        Objects.requireNonNull(inicio, "El inicio del intervalo es obligatorio");
        Objects.requireNonNull(fin, "El fin del intervalo es obligatorio");

        return ejecutar("Cancelar citas confirmadas solapadas", connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement(CANCELAR_SOLAPAMIENTOS_CONFIRMADOS)) {
                statement.setObject(1, fin);
                statement.setObject(2, inicio);
                return statement.executeUpdate();
            }
        });
    }

    private Cita mapearCita(ResultSet resultSet) throws SQLException {
        return new Cita(
                resultSet.getInt("id"),
                resultSet.getInt("usuario_id"),
                resultSet.getObject("inicio", LocalDateTime.class),
                resultSet.getObject("fin", LocalDateTime.class),
                EstadoCita.valueOf(resultSet.getString("estado")),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class)
        );
    }

    private List<Cita> mapearCitas(ResultSet resultSet) throws SQLException {
        List<Cita> citas = new ArrayList<>();
        while (resultSet.next()) {
            citas.add(mapearCita(resultSet));
        }
        return List.copyOf(citas);
    }

    private <T> T ejecutar(String tarea, OperacionJdbc<T> operacion) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            return operacion.ejecutar(connection);
        } catch (SQLException exception) {
            throw new UncategorizedSQLException(tarea, null, exception);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @FunctionalInterface
    private interface OperacionJdbc<T> {
        T ejecutar(Connection connection) throws SQLException;
    }
}
