package dev.eduardo.artemedica.farmacia.dto;

import dev.eduardo.artemedica.farmacia.model.enums.Rol;

import java.time.LocalDateTime;

public record UsuarioResponseDTO(
        Long id, String username, Long empleadoId, String empleadoNombre,
        Rol rol, boolean activo, LocalDateTime createdAt, LocalDateTime updatedAt
) {}
