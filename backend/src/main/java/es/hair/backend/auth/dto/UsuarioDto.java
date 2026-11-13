package es.hair.backend.auth.dto;

import es.hair.backend.auth.Rol;
import es.hair.backend.auth.Usuario;

public record UsuarioDto(long id, String nombre, String apellidos, String email, String telefono, Rol rol) {

    public static UsuarioDto desde(Usuario usuario) {
        return new UsuarioDto(usuario.id(), usuario.nombre(), usuario.apellidos(),
                usuario.email(), usuario.telefono(), usuario.rol());
    }
}
