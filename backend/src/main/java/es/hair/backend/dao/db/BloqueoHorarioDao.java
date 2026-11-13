package es.hair.backend.dao.db;

import es.hair.backend.exception.ErrorPersistenciaException;
import es.hair.backend.model.BloqueoHorario;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class BloqueoHorarioDao {

    private static final String COLUMNAS = "id, fecha, hora_inicio, hora_fin, motivo, created_at";
    private final DataSource dataSource;

    public BloqueoHorarioDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<BloqueoHorario> findByRango(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT " + COLUMNAS + " FROM bloqueo_horario "
                + "WHERE fecha BETWEEN ? AND ? ORDER BY fecha, hora_inicio";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            return leerLista(ps);
        } catch (SQLException e) {
            throw new ErrorPersistenciaException("Error leyendo bloqueos horarios", e);
        }
    }

    public List<BloqueoHorario> findByFecha(LocalDate fecha) {
        return findByRango(fecha, fecha);
    }

    public Optional<BloqueoHorario> findById(long id) {
        String sql = "SELECT " + COLUMNAS + " FROM bloqueo_horario WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            List<BloqueoHorario> lista = leerLista(ps);
            return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
        } catch (SQLException e) {
            throw new ErrorPersistenciaException("Error leyendo bloqueo horario", e);
        }
    }

    /** Bloqueos de un día que solapan con [inicio, fin). Solapan si ini < finBuscado y fin > iniBuscado. */
    public List<BloqueoHorario> findSolapados(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        String sql = "SELECT " + COLUMNAS + " FROM bloqueo_horario "
                + "WHERE fecha = ? AND hora_inicio < ? AND hora_fin > ? ORDER BY hora_inicio";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            ps.setTime(2, Time.valueOf(fin));
            ps.setTime(3, Time.valueOf(inicio));
            return leerLista(ps);
        } catch (SQLException e) {
            throw new ErrorPersistenciaException("Error buscando bloqueos solapados", e);
        }
    }

    public long insert(Connection con, LocalDate fecha, LocalTime inicio, LocalTime fin, String motivo)
            throws SQLException {
        String sql = "INSERT INTO bloqueo_horario (fecha, hora_inicio, hora_fin, motivo) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(fecha));
            ps.setTime(2, Time.valueOf(inicio));
            ps.setTime(3, Time.valueOf(fin));
            ps.setString(4, motivo);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public boolean deleteById(Connection con, long id) throws SQLException {
        String sql = "DELETE FROM bloqueo_horario WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private List<BloqueoHorario> leerLista(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<BloqueoHorario> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(new BloqueoHorario(
                        rs.getLong("id"),
                        rs.getDate("fecha").toLocalDate(),
                        rs.getTime("hora_inicio").toLocalTime(),
                        rs.getTime("hora_fin").toLocalTime(),
                        rs.getString("motivo"),
                        rs.getTimestamp("created_at").toLocalDateTime()));
            }
            return lista;
        }
    }
}
