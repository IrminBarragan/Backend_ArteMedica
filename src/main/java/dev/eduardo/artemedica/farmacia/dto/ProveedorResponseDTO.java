package dev.eduardo.artemedica.farmacia.dto;

public record ProveedorResponseDTO(
        Long id, String nombre, String direccion, String telefono, String correo, boolean activo
) {}
