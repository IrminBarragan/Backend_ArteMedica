package dev.eduardo.artemedica.farmacia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Baja de unidades por caducidad, rotura, robo o cualquier perdida. */
public record MermaRequestDTO(
        @NotNull Long loteId,
        @NotNull @Positive Integer cantidad,
        @NotBlank String motivo
) {}
