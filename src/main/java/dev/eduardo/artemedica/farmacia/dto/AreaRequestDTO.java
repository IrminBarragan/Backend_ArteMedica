package dev.eduardo.artemedica.farmacia.dto;

import jakarta.validation.constraints.NotBlank;

public record AreaRequestDTO(@NotBlank String nombre, String descripcion) {}
