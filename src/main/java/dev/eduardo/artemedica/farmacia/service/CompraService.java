package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.CompraRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.CompraResponseDTO;

import java.time.LocalDate;
import dev.eduardo.artemedica.farmacia.dto.PaginaDTO;
import org.springframework.data.domain.Pageable;

public interface CompraService {
    CompraResponseDTO registrarCompra(CompraRequestDTO dto, Long usuarioId);
    CompraResponseDTO obtenerPorId(Long id);
    PaginaDTO<CompraResponseDTO> listar(Long proveedorId, LocalDate desde, LocalDate hasta, Pageable pageable);
}
