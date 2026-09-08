package dev.eduardo.artemedica.farmacia.dto;

public record RefreshResponseDTO(
        String token,
        String tipo,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn
) {}
