package es.hair.backend.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import es.hair.backend.auth.dto.AuthResponse;
import es.hair.backend.auth.dto.GoogleLoginRequest;
import es.hair.backend.auth.dto.LoginRequest;
import es.hair.backend.auth.dto.RegistroRequest;
import es.hair.backend.auth.dto.UsuarioDto;
import es.hair.backend.auth.exception.CredencialesInvalidasException;
import es.hair.backend.auth.exception.GoogleTokenInvalidoException;
import es.hair.backend.auth.exception.SolicitudInvalidaException;
import es.hair.backend.auth.exception.UsuarioDuplicadoException;
import es.hair.backend.auth.security.JwtTokenService;
import es.hair.backend.auth.security.UsuarioAutenticado;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UsuarioDao usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;
    private final GoogleIdTokenVerifierService google;
    private final String hashDummy;

    public AuthService(UsuarioDao usuarios, PasswordEncoder passwordEncoder, JwtTokenService tokens,
                       GoogleIdTokenVerifierService google) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.google = google;
        this.hashDummy = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public AuthResponse registrar(RegistroRequest request) {
        String password = request.password();
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new SolicitudInvalidaException("La contraseña supera el tamaño máximo admitido.");
        }
        Usuario usuario = usuarios.crearConPassword(
                request.nombre().trim(),
                request.apellidos().trim(),
                normalizarEmail(request.email()),
                request.telefono() == null || request.telefono().isBlank() ? null : request.telefono().trim(),
                passwordEncoder.encode(password));
        return respuesta(usuario);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizarEmail(request.email());
        Optional<Usuario> encontrado = usuarios.buscarPorEmail(email);
        Usuario usuario = encontrado.orElse(null);
        String hash = usuario == null || usuario.passwordHash() == null ? hashDummy : usuario.passwordHash();
        boolean passwordCorrecta = passwordEncoder.matches(request.password(), hash);
        if (!passwordCorrecta || usuario == null || !usuario.activo() || usuario.passwordHash() == null) {
            throw new CredencialesInvalidasException();
        }
        return respuesta(usuario);
    }

    public AuthResponse loginGoogle(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = google.verificar(request.idToken());
        String sub = payload.getSubject();
        String email = normalizarEmail(payload.getEmail());
        Optional<Usuario> existente = usuarios.buscarPorGoogleSub(sub);

        if (existente.isPresent()) {
            Usuario usuario = existente.get();
            if (!usuario.activo()) {
                throw new CredencialesInvalidasException();
            }
            return respuesta(usuario);
        }

        String nombre = texto(payload.get("given_name"));
        String apellidos = texto(payload.get("family_name"));
        if (nombre.isBlank() || apellidos.isBlank()) {
            String[] partes = texto(payload.get("name")).trim().split("\\s+", 2);
            if (nombre.isBlank() && partes.length > 0) {
                nombre = partes[0];
            }
            if (apellidos.isBlank() && partes.length > 1) {
                apellidos = partes[1];
            }
        }
        if (nombre.isBlank()) {
            nombre = "Usuario";
        }
        if (apellidos.isBlank()) {
            apellidos = "Google";
        }

        try {
            return respuesta(usuarios.crearConGoogle(nombre, apellidos, email, sub));
        } catch (UsuarioDuplicadoException e) {
            Usuario trasCarrera = usuarios.buscarPorGoogleSub(sub).orElse(null);
            if (trasCarrera != null && trasCarrera.activo()) {
                return respuesta(trasCarrera);
            }
            throw e;
        }
    }

    public UsuarioDto usuarioActual(UsuarioAutenticado autenticado) {
        Usuario usuario = usuarios.buscarPorId(autenticado.id())
                .filter(Usuario::activo)
                .orElseThrow(CredencialesInvalidasException::new);
        return UsuarioDto.desde(usuario);
    }

    private AuthResponse respuesta(Usuario usuario) {
        return new AuthResponse(tokens.emitir(usuario), "Bearer",
                JwtTokenService.EXPIRACION_SEGUNDOS, UsuarioDto.desde(usuario));
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String texto(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
