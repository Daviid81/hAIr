package es.hair.backend.dao.db;

import es.hair.backend.exception.ErrorPersistenciaException;
import es.hair.backend.model.ArchivoBloqueo;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

@Repository
public class ArchivoBloqueoDao {

    private final DataSource dataSource;

    public ArchivoBloqueoDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Participa en una transacción. Devuelve el id generado. */
    public long insert(Connection con, ArchivoBloqueo a) throws SQLException {
        String sql = "INSERT INTO archivo_bloqueo (admin_id, nombre_original, nombre_almacenado, "
                + "tamano_bytes, hash_sha256, mes, anio, dias_importados) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, a.adminId());
            ps.setString(2, a.nombreOriginal());
            ps.setString(3, a.nombreAlmacenado());
            ps.setLong(4, a.tamanoBytes());
            ps.setString(5, a.hashSha256());
            ps.setInt(6, a.mes());
            ps.setInt(7, a.anio());
            ps.setInt(8, a.diasImportados());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /** «Mes importado» = existe al menos un archivo para ese mes y año. */
    public boolean existsByMesAnio(int mes, int anio) {
        String sql = "SELECT 1 FROM archivo_bloqueo WHERE mes = ? AND anio = ? LIMIT 1";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, mes);
            ps.setInt(2, anio);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new ErrorPersistenciaException("Error comprobando mes importado", e);
        }
    }

    public Optional<ArchivoBloqueo> findUltimoByMesAnio(int mes, int anio) {
        String sql = "SELECT id, admin_id, nombre_original, nombre_almacenado, tamano_bytes, hash_sha256, "
                + "mes, anio, dias_importados, created_at FROM archivo_bloqueo "
                + "WHERE mes = ? AND anio = ? ORDER BY created_at DESC, id DESC LIMIT 1";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, mes);
            ps.setInt(2, anio);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new ArchivoBloqueo(
                        rs.getLong("id"), rs.getLong("admin_id"),
                        rs.getString("nombre_original"), rs.getString("nombre_almacenado"),
                        rs.getLong("tamano_bytes"), rs.getString("hash_sha256"),
                        rs.getInt("mes"), rs.getInt("anio"), rs.getInt("dias_importados"),
                        rs.getTimestamp("created_at").toLocalDateTime()));
            }
        } catch (SQLException e) {
            throw new ErrorPersistenciaException("Error leyendo archivo de bloqueo", e);
        }
    }
}
