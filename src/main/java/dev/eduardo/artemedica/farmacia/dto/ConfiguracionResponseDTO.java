package dev.eduardo.artemedica.farmacia.dto;

import java.time.LocalDateTime;

public record ConfiguracionResponseDTO(
        String clave, String valor, String descripcion, LocalDateTime updatedAt, String updatedBy
) {}
