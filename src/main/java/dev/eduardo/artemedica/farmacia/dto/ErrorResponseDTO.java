package dev.eduardo.artemedica.farmacia.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponseDTO(
        String mensaje,
        int status,
        LocalDateTime timestamp,
        Map<String, String> errores
) {}
