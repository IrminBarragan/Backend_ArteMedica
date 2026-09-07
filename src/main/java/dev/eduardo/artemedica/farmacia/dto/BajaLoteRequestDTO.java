package dev.eduardo.artemedica.farmacia.dto;

import jakarta.validation.constraints.NotBlank;

/** Motivo por el que se da de baja un lote completo, ej. "caducado" o "cadena de frio rota". */
public record BajaLoteRequestDTO(@NotBlank String motivo) {}
