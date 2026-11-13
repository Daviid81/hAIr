package es.hair.backend.model;

import java.time.LocalDateTime;

public record ArchivoBloqueo(Long id, Long adminId, String nombreOriginal,
                             String nombreAlmacenado, long tamanoBytes, String hashSha256,
                             int mes, int anio, int diasImportados, LocalDateTime createdAt) {}