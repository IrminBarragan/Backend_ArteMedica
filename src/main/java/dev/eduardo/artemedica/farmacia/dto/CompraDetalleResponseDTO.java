package dev.eduardo.artemedica.farmacia.dto;

import java.math.BigDecimal;

public record CompraDetalleResponseDTO(
        Long productoId, String productoNombre, Integer cantidad,
        BigDecimal costoUnitario, BigDecimal subtotal, Long loteId, String numeroLote
) {}
