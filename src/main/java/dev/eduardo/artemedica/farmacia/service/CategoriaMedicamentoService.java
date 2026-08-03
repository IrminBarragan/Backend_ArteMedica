package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.CategoriaMedicamentoRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.CategoriaMedicamentoResponseDTO;

import java.util.List;

public interface CategoriaMedicamentoService {
    CategoriaMedicamentoResponseDTO crear(CategoriaMedicamentoRequestDTO dto);
    CategoriaMedicamentoResponseDTO actualizar(Long id, CategoriaMedicamentoRequestDTO dto);
    CategoriaMedicamentoResponseDTO obtenerPorId(Long id);
    List<CategoriaMedicamentoResponseDTO> listarActivos();
    void desactivar(Long id);
}
