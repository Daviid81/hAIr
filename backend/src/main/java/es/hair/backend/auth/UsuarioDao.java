package es.hair.backend.auth;

import es.hair.backend.auth.exception.RepositorioException;
import es.hair.backend.auth.exception.UsuarioDuplicadoException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class UsuarioDao {

    private static final String SELECT_COLUMNS =
            "id, nombre, apellidos, email, telefono, password_hash, google_sub, rol, activo, created_at";

    private final DataSource dataSource;

    public UsuarioDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return buscar("SELECT " + SELECT_COLUMNS + " FROM usuario WHERE email = ?", email);
    }

    public Optional<Usuario> buscarPorGoogleSub(String googleSub) {
        return buscar("SELECT " + SELECT_COLUMNS + " FROM usuario WHERE google_sub = ?", googleSub);
    }

    public Optional<Usuario> buscarPorId(long id) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM usuario WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapear(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo consultar el usuario", e);
        }
    }

    public Usuario crearConPassword(String nombre, String apellidos, String email,
                                    String telefono, String passwordHash) {
        String sql = """
                INSERT INTO usuario (nombre, apellidos, email, telefono, password_hash, rol, activo)
                VALUES (?, ?, ?, ?, ?, 'CLIENTE', TRUE)
                """;
        return insertar(sql, statement -> {
            statement.setString(1, nombre);
            statement.setString(2, apellidos);
            statement.setString(3, email);
            statement.setString(4, telefono);
            statement.setString(5, passwordHash);
        });
    }

    public Usuario crearConGoogle(String nombre, String apellidos, String email, String googleSub) {
        String sql = """
                INSERT INTO usuario (nombre, apellidos, email, google_sub, rol, activo)
                VALUES (?, ?, ?, ?, 'CLIENTE', TRUE)
                """;
        return insertar(sql, statement -> {
            statement.setString(1, nombre);
            statement.setString(2, apellidos);
            statement.setString(3, email);
            statement.setString(4, googleSub);
        });
    }

    private Optional<Usuario> buscar(String sql, String valor) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, valor);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapear(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositorioException("No se pudo consultar el usuario", e);
        }
    }

    private Usuario insertar(String sql, Preparador preparador) {
        long id;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparador.preparar(statement);
            if (statement.executeUpdate() != 1) {
                throw new RepositorioException("No se pudo crear el usuario");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new RepositorioException("La base de datos no devolvió el id del usuario");
                }
                id = keys.getLong(1);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new UsuarioDuplicadoException();
            }
            throw new RepositorioException("No se pudo crear el usuario", e);
        }
        return buscarPorId(id)
                .orElseThrow(() -> new RepositorioException("No se encontró el usuario recién creado"));
    }

    private Usuario mapear(ResultSet result) throws SQLException {
        Timestamp createdAt = result.getTimestamp("created_at");
        return new Usuario(
                result.getLong("id"),
                result.getString("nombre"),
                result.getString("apellidos"),
                result.getString("email"),
                result.getString("telefono"),
                result.getString("password_hash"),
                result.getString("google_sub"),
                Rol.valueOf(result.getString("rol")),
                result.getBoolean("activo"),
                createdAt == null ? null : createdAt.toLocalDateTime());
    }

    @FunctionalInterface
    private interface Preparador {
        void preparar(PreparedStatement statement) throws SQLException;
    }
}
