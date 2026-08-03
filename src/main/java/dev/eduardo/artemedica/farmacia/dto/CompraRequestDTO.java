package dev.eduardo.artemedica.farmacia.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CompraRequestDTO(
        @NotNull Long proveedorId,
        @NotBlank String numeroFactura,
        @NotNull LocalDate fechaCompra,
        @NotEmpty @Valid List<CompraDetalleRequestDTO> detalles
) {}
