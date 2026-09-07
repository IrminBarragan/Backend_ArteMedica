package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.CategoriaMedicamentoRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.CategoriaMedicamentoResponseDTO;
import dev.eduardo.artemedica.farmacia.dto.PaginaDTO;
import org.springframework.data.domain.Pageable;

public interface CategoriaMedicamentoService {
    CategoriaMedicamentoResponseDTO crear(CategoriaMedicamentoRequestDTO dto);
    CategoriaMedicamentoResponseDTO actualizar(Long id, CategoriaMedicamentoRequestDTO dto);
    CategoriaMedicamentoResponseDTO obtenerPorId(Long id);
    PaginaDTO<CategoriaMedicamentoResponseDTO> listarActivos(Pageable pageable);
    void desactivar(Long id);
}
