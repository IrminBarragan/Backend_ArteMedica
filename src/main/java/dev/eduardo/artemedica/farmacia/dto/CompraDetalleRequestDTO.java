package dev.eduardo.artemedica.farmacia.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompraDetalleRequestDTO(
        @NotNull Long productoId,
        @NotBlank String numeroLote,
        @NotNull @Future LocalDate fechaCaducidad,
        @NotNull @Positive Integer cantidad,
        @NotNull @Positive BigDecimal costoUnitario
) {}
