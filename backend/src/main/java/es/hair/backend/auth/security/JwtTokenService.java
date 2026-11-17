package es.hair.backend.auth.security;

import es.hair.backend.auth.Rol;
import es.hair.backend.auth.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtTokenService {

    public static final long EXPIRACION_SEGUNDOS = Duration.ofHours(8).toSeconds();

    private final SecretKey signingKey;

    public JwtTokenService(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(Long.toString(usuario.id()))
                .claim("email", usuario.email())
                .claim("rol", usuario.rol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(EXPIRACION_SEGUNDOS)))
                .signWith(signingKey)
                .compact();
    }

    public UsuarioAutenticado validar(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new UsuarioAutenticado(
                Long.parseLong(claims.getSubject()),
                claims.get("email", String.class),
                Rol.valueOf(claims.get("rol", String.class)));
    }
}
