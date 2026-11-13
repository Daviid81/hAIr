package es.hair.backend.controller;

import es.hair.backend.auth.AuthService;
import es.hair.backend.auth.dto.AuthResponse;
import es.hair.backend.auth.dto.GoogleLoginRequest;
import es.hair.backend.auth.dto.LoginRequest;
import es.hair.backend.auth.dto.RegistroRequest;
import es.hair.backend.auth.dto.UsuarioDto;
import es.hair.backend.auth.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registrar(@Valid @RequestBody RegistroRequest request) {
        return auth.registrar(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @PostMapping("/google")
    public AuthResponse loginGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return auth.loginGoogle(request);
    }

    @GetMapping("/me")
    public UsuarioDto me(@AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return auth.usuarioActual(autenticado);
    }
}
