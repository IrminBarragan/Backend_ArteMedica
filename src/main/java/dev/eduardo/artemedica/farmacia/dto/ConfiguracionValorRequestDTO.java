package dev.eduardo.artemedica.farmacia.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfiguracionValorRequestDTO(@NotBlank String valor) {}
