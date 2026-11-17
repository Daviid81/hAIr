package es.hair.backend.auth.dto;

public record AuthResponse(String token, String tipo, long expiraEnSegundos, UsuarioDto usuario) {
}
