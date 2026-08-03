package dev.eduardo.artemedica.farmacia.dto;

import dev.eduardo.artemedica.farmacia.model.enums.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequestDTO(
        @NotBlank String username,
        @NotBlank @Size(min = 8) String password,
        @NotNull Long empleadoId,
        @NotNull Rol rol
) {}
