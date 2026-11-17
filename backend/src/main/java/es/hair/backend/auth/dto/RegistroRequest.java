package es.hair.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 150) String apellidos,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 30) String telefono,
        @NotBlank @Size(min = 8, max = 72) String password) {
}
