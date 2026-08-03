package dev.eduardo.artemedica.farmacia.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoteResponseDTO(
        Long id, String numeroLote, Long productoId, String productoNombre,
        Long proveedorId, String proveedorNombre, LocalDate fechaCaducidad,
        BigDecimal costoCompra, Integer cantidadInicial, Integer existenciaActual, boolean activo
) {}
