package dev.eduardo.artemedica.farmacia.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoriaMedicamentoRequestDTO(@NotBlank String nombre, String descripcion) {}
