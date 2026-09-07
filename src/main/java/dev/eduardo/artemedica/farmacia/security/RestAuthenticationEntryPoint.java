package dev.eduardo.artemedica.farmacia.security;

import tools.jackson.databind.ObjectMapper;
import dev.eduardo.artemedica.farmacia.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Responde 401 con el mismo cuerpo JSON que el resto de la API cuando la peticion llega
 * sin autenticar (token ausente, expirado o invalido).
 *
 * Sin este componente Spring Security respondia 403 con el cuerpo vacio, y el cliente no
 * podia distinguir "tu sesion caduco, vuelve a iniciar sesion" de "no tienes permisos".
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorResponseDTO body = new ErrorResponseDTO(
                "No estas autenticado. Inicia sesion o renueva tu token.",
                HttpStatus.UNAUTHORIZED.value(),
                LocalDateTime.now(),
                null);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
