package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.LoteResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface LoteService {
    LoteResponseDTO obtenerPorId(Long id);
    List<LoteResponseDTO> listarActivos();
    List<LoteResponseDTO> listarPorProducto(Long productoId);
    List<LoteResponseDTO> listarVencidos();
    List<LoteResponseDTO> listarPorVencer(LocalDate fechaLimite);
    void desactivar(Long id);
}
