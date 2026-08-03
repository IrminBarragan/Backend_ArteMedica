package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.CompraRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.CompraResponseDTO;

public interface CompraService {
    CompraResponseDTO registrarCompra(CompraRequestDTO dto, Long usuarioId);
}
