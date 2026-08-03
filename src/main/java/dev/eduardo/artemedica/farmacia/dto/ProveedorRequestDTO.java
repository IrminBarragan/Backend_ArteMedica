package dev.eduardo.artemedica.farmacia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProveedorRequestDTO(
        @NotBlank String nombre, String direccion, String telefono, @Email String correo
) {}
