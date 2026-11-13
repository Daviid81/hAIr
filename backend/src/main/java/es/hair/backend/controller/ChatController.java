package es.hair.backend.controller;

import es.hair.backend.ai.AiUnavailableException;
import es.hair.backend.ai.AssistantService;
import es.hair.backend.ai.ChatRequestInvalidaException;
import es.hair.backend.ai.dto.ChatRequest;
import es.hair.backend.ai.dto.ChatResponse;
import es.hair.backend.auth.security.UsuarioAutenticado;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Temporal: usa el usuario autenticado hasta que se integre el servicio real de citas. */
@RestController
@RequestMapping("/api/chat")
@Profile("ia-stub")
public class ChatController {

    private final AssistantService assistant;

    public ChatController(AssistantService assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/mensajes")
    public ChatResponse mensajes(@AuthenticationPrincipal UsuarioAutenticado usuario, @RequestBody ChatRequest request) {
        return assistant.responder(usuario.id(), request.mensajes());
    }

    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<Map<String, String>> iaNoDisponible(AiUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
                .body(Map.of("mensaje",
                        "El asistente está muy ocupado ahora mismo. Inténtalo en un minuto o reserva con el calendario."));
    }

    @ExceptionHandler(ChatRequestInvalidaException.class)
    public ResponseEntity<Map<String, String>> peticionInvalida(ChatRequestInvalidaException e) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
    }
}
