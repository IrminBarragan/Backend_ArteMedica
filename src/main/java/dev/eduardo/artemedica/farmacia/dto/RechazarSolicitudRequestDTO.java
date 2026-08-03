package dev.eduardo.artemedica.farmacia.dto;

import jakarta.validation.constraints.NotBlank;

public record RechazarSolicitudRequestDTO(@NotBlank String motivo) {}
