package dev.eduardo.artemedica.farmacia.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CompraResponseDTO(
        Long id, Long proveedorId, String proveedorNombre, String numeroFactura,
        LocalDate fechaCompra, String usuarioRegistroUsername, BigDecimal total,
        List<CompraDetalleResponseDTO> detalles, LocalDateTime createdAt
) {}
