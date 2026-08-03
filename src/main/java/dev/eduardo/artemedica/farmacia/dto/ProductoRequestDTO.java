package dev.eduardo.artemedica.farmacia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductoRequestDTO(
        @NotBlank String nombre,
        @NotBlank String presentacion,
        String codigoBarras,
        boolean esControlado,
        @NotNull Long categoriaId,
        @NotNull @Positive BigDecimal precioVenta,
        @NotNull @Positive BigDecimal precioCompra,
        @NotNull @PositiveOrZero Integer stockMinimo
) {}
