package dev.eduardo.artemedica.farmacia.security;

import tools.jackson.databind.ObjectMapper;
import dev.eduardo.artemedica.farmacia.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Responde 403 con cuerpo JSON cuando el usuario esta autenticado pero su rol no alcanza.
 *
 * El GlobalExceptionHandler ya cubre las AccessDeniedException lanzadas por @PreAuthorize
 * dentro de los controllers; este handler cubre las que se producen en la cadena de filtros,
 * antes de llegar al controller.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ErrorResponseDTO body = new ErrorResponseDTO(
                "No tienes permisos para realizar esta accion",
                HttpStatus.FORBIDDEN.value(),
                LocalDateTime.now(),
                null);

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
