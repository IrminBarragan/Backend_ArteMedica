package dev.eduardo.artemedica.farmacia.dto;

import dev.eduardo.artemedica.farmacia.model.enums.Rol;

public record LoginResponseDTO(String token, String tipo, String username, Rol rol, Long empleadoId, long expiresIn) {}
