package dev.eduardo.artemedica.farmacia.dto;

import java.time.LocalDateTime;

public record CodigoEquivalenteResponseDTO(
        Long id, Long productoId, String productoNombre, String codigoBarras,
        boolean activo, LocalDateTime createdAt, String createdBy
) {}
