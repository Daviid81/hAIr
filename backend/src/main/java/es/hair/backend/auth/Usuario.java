package es.hair.backend.auth;

import java.time.LocalDateTime;

public record Usuario(
        long id,
        String nombre,
        String apellidos,
        String email,
        String telefono,
        String passwordHash,
        String googleSub,
        Rol rol,
        boolean activo,
        LocalDateTime createdAt) {
}
