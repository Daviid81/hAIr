package es.hair.backend.auth.security;

import es.hair.backend.auth.Rol;

public record UsuarioAutenticado(long id, String email, Rol rol) {
}
