package dev.eduardo.artemedica.farmacia.exception;

import dev.eduardo.artemedica.farmacia.dto.ErrorResponseDTO;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponseDTO> handleReglaNegocio(ReglaNegocioException e) {
        return construir(HttpStatus.BAD_REQUEST, e.getMessage(), null);
    }

    @ExceptionHandler(AutenticacionException.class)
    public ResponseEntity<ErrorResponseDTO> handleAutenticacion(AutenticacionException e) {
        return construir(HttpStatus.UNAUTHORIZED, e.getMessage(), null);
    }

    @ExceptionHandler(RefreshTokenInvalidoException.class)
    public ResponseEntity<ErrorResponseDTO> handleRefreshTokenInvalido(RefreshTokenInvalidoException e) {
        return construir(HttpStatus.UNAUTHORIZED, e.getMessage(), null);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponseDTO> handleJwt(JwtException e) {
        return construir(HttpStatus.UNAUTHORIZED, "Token invalido o expirado", null);
    }

    /**
     * Choque contra una restriccion de la base de datos, tipicamente un valor unico repetido
     * (username, codigo de barras) o una llave foranea. Sin este handler caia en el generico
     * y el cliente recibia un 500 opaco en lugar de saber que el dato ya existe.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleIntegridad(DataIntegrityViolationException e) {
        log.warn("Violacion de integridad de datos: {}", e.getMostSpecificCause().getMessage());
        return construir(HttpStatus.CONFLICT,
                "El dato que intentas guardar entra en conflicto con uno existente. "
                        + "Revisa los campos que deben ser unicos, como el nombre de usuario o el codigo de barras.",
                null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidacion(MethodArgumentNotValidException e) {
        Map<String, String> errores = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errores.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return construir(HttpStatus.BAD_REQUEST, "Error de validacion", errores);
    }

    /** JSON mal formado o con un tipo que no encaja en el DTO. Es culpa del cliente, no del servidor. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleJsonIlegible(HttpMessageNotReadableException e) {
        return construir(HttpStatus.BAD_REQUEST,
                "El cuerpo de la peticion no se pudo leer. Revisa que sea JSON valido y que los tipos coincidan.",
                null);
    }

    /** Un path variable o query param con un tipo incorrecto, ej. /api/productos/abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTipoInvalido(MethodArgumentTypeMismatchException e) {
        return construir(HttpStatus.BAD_REQUEST,
                "El parametro '" + e.getName() + "' tiene un valor invalido: " + e.getValue(), null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleParametroFaltante(MissingServletRequestParameterException e) {
        return construir(HttpStatus.BAD_REQUEST,
                "Falta el parametro obligatorio '" + e.getParameterName() + "'.", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleRutaInexistente(NoResourceFoundException e) {
        return construir(HttpStatus.NOT_FOUND, "La ruta solicitada no existe.", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException e) {
        return construir(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta accion", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenerica(Exception e) {
        // Se registra el stacktrace completo: es la unica pista cuando algo falla en produccion.
        log.error("Error no controlado", e);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado", null);
    }

    private ResponseEntity<ErrorResponseDTO> construir(HttpStatus status, String mensaje, Map<String, String> errores) {
        ErrorResponseDTO body = new ErrorResponseDTO(mensaje, status.value(), LocalDateTime.now(), errores);
        return ResponseEntity.status(status).body(body);
    }
}
