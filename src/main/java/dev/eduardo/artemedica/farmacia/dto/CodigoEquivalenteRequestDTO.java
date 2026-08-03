package dev.eduardo.artemedica.farmacia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CodigoEquivalenteRequestDTO(@NotNull Long productoId, @NotBlank String codigoBarras) {}
