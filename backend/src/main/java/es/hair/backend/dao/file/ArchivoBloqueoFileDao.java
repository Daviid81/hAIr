package es.hair.backend.dao.file;

import es.hair.backend.exception.ErrorPersistenciaException;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Repository
public class ArchivoBloqueoFileDao {

    private static final Logger log = LoggerFactory.getLogger(ArchivoBloqueoFileDao.class);
    private final Path directorio;

    public ArchivoBloqueoFileDao(@Value("${app.upload-dir}") String uploadDir) {
        this.directorio = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directorio);
        } catch (IOException e) {
            throw new ErrorPersistenciaException("No se pudo crear el directorio de archivos", e);
        }
    }

    /** Guarda el contenido con un nombre generado y devuelve ese nombre. */
    public String guardar(byte[] contenido) {
        String nombre = UUID.randomUUID() + ".xlsx";
        try {
            Files.write(resolver(nombre), contenido, StandardOpenOption.CREATE_NEW);
            return nombre;
        } catch (IOException e) {
            throw new ErrorPersistenciaException("No se pudo guardar el archivo", e);
        }
    }

    public byte[] leer(String nombreAlmacenado) {
        try {
            return Files.readAllBytes(resolver(nombreAlmacenado));
        } catch (IOException e) {
            throw new ErrorPersistenciaException("No se pudo leer el archivo", e);
        }
    }

    /** Borrado silencioso: se usa para limpiar archivos huérfanos, no debe romper el flujo. */
    public void borrar(String nombreAlmacenado) {
        try {
            Files.deleteIfExists(resolver(nombreAlmacenado));
        } catch (IOException e) {
            log.warn("No se pudo borrar el archivo huérfano {}", nombreAlmacenado, e);
        }
    }

    private Path resolver(String nombre) {
        Path destino = directorio.resolve(nombre).normalize();
        if (!destino.startsWith(directorio)) {
            throw new IllegalArgumentException("Nombre de archivo no permitido");
        }
        return destino;
    }
}
