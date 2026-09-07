package dev.eduardo.artemedica.farmacia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Correccion manual de existencias tras un conteo fisico.
 * Una cantidad positiva suma unidades al lote y una negativa las resta.
 */
public record AjusteRequestDTO(
        @NotNull Long loteId,
        @NotNull Integer cantidad,
        @NotBlank String motivo
) {}
