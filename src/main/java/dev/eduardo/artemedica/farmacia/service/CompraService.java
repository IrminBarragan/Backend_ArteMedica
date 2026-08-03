package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.CompraRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.CompraResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface CompraService {
    CompraResponseDTO registrarCompra(CompraRequestDTO dto, Long usuarioId);
    CompraResponseDTO obtenerPorId(Long id);
    List<CompraResponseDTO> listar(Long proveedorId, LocalDate desde, LocalDate hasta);
}
