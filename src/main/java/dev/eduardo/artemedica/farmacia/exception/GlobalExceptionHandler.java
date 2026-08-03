package dev.eduardo.artemedica.farmacia.exception;

import dev.eduardo.artemedica.farmacia.dto.ErrorResponseDTO;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(ResourceNotFoundException e) {
        return construir(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<ErrorResponseDTO> handleStockInsuficiente(StockInsuficienteException e) {
        return construir(HttpStatus.CONFLICT, e.getMessage(), null);
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public ResponseEntity<ErrorResponseDTO> handleEstadoInvalido(EstadoInvalidoException e) {
        return construir(HttpStatus.CONFLICT, e.getMessage(), null);
    }

    @ExceptionHandler(ConflictoConcurrenciaException.class)
    public ResponseEntity<ErrorResponseDTO> handleConflictoConcurrencia(ConflictoConcurrenciaException e) {
        return construir(HttpStatus.CONFLICT, e.getMessage(), null);
    }

    @ExceptionHandler(AutenticacionException.class)
    public ResponseEntity<ErrorResponseDTO> handleAutenticacion(AutenticacionException e) {
        return construir(HttpStatus.UNAUTHORIZED, e.getMessage(), null);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponseDTO> handleJwt(JwtException e) {
        return construir(HttpStatus.UNAUTHORIZED, "Token invalido o expirado", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidacion(MethodArgumentNotValidException e) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errores.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return construir(HttpStatus.BAD_REQUEST, "Error de validacion", errores);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException e) {
        return construir(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta accion", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenerica(Exception e) {
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado", null);
    }

    private ResponseEntity<ErrorResponseDTO> construir(HttpStatus status, String mensaje, Map<String, String> errores) {
        ErrorResponseDTO body = new ErrorResponseDTO(mensaje, status.value(), LocalDateTime.now(), errores);
        return ResponseEntity.status(status).body(body);
    }
}
