package es.hair.backend.controller;

import es.hair.backend.auth.exception.CredencialesInvalidasException;
import es.hair.backend.auth.exception.GoogleTokenInvalidoException;
import es.hair.backend.auth.exception.RepositorioException;
import es.hair.backend.auth.exception.SolicitudInvalidaException;
import es.hair.backend.auth.exception.UsuarioDuplicadoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, SolicitudInvalidaException.class})
    public ResponseEntity<Map<String, String>> solicitudInvalida(Exception e) {
        return respuesta(HttpStatus.BAD_REQUEST, "La solicitud no es válida.");
    }

    @ExceptionHandler({CredencialesInvalidasException.class, GoogleTokenInvalidoException.class})
    public ResponseEntity<Map<String, String>> noAutorizado(Exception e) {
        return respuesta(HttpStatus.UNAUTHORIZED, "Las credenciales no son válidas.");
    }

    @ExceptionHandler(UsuarioDuplicadoException.class)
    public ResponseEntity<Map<String, String>> usuarioDuplicado(UsuarioDuplicadoException e) {
        return respuesta(HttpStatus.CONFLICT, "Ya existe una cuenta con esos datos.");
    }

    @ExceptionHandler(RepositorioException.class)
    public ResponseEntity<Map<String, String>> errorRepositorio(RepositorioException e) {
        log.error("Error interno al acceder a los datos de usuario", e);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la solicitud.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> errorNoControlado(Exception e) {
        log.error("Error no controlado en la API", e);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la solicitud.");
    }

    private ResponseEntity<Map<String, String>> respuesta(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("mensaje", mensaje));
    }
}
