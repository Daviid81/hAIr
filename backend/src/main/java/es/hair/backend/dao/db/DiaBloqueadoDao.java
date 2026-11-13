package es.hair.backend.dao.db;

import es.hair.backend.exception.ErrorPersistenciaException;
import es.hair.backend.model.DiaBloqueado;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DiaBloqueadoDao {

    private final DataSource dataSource;

    public DiaBloqueadoDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<DiaBloqueado> findByRango(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT id, fecha, motivo, archivo_id, created_at FROM dia_bloqueado "
                + "WHERE fecha BETWEEN ? AND ? ORDER BY fecha";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                List<DiaBloqueado> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(new DiaBloqueado(
                            rs.getLong("id"),
                            rs.getDate("fecha").toLocalDate(),
                            rs.getString("motivo"),
                            (Long) rs.getObject("archivo_id"),
                            rs.getTimestamp("created_at").toLocalDateTime()));
                }
                return lista;
            }
        } catch (SQLException e) {
            throw new ErrorPersistenciaException("Error leyendo días bloqueados", e);
        }
    }

    public boolean existsByFecha(LocalDate fecha) {
        String sql = "SELECT 1 FROM dia_bloqueado WHERE fecha = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ErrorPersistenciaException("Error comprobando día bloqueado", e);
        }
    }

    /** Participa en una transacción: usa la conexión recibida y NO la cierra. */
    public void deleteByRango(Connection con, LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "DELETE FROM dia_bloqueado WHERE fecha BETWEEN ? AND ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ps.executeUpdate();
        }
    }

    /** Participa en una transacción. */
    public void insert(Connection con, LocalDate fecha, String motivo, Long archivoId) throws SQLException {
        String sql = "INSERT INTO dia_bloqueado (fecha, motivo, archivo_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            ps.setString(2, motivo);
            if (archivoId == null) ps.setNull(3, Types.BIGINT); else ps.setLong(3, archivoId);
            ps.executeUpdate();
        }
    }
}
