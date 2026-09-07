package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.AreaRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.AreaResponseDTO;
import dev.eduardo.artemedica.farmacia.dto.PaginaDTO;
import org.springframework.data.domain.Pageable;

public interface AreaService {
    AreaResponseDTO crear(AreaRequestDTO dto);
    AreaResponseDTO actualizar(Long id, AreaRequestDTO dto);
    AreaResponseDTO obtenerPorId(Long id);
    PaginaDTO<AreaResponseDTO> listarActivos(Pageable pageable);
    void desactivar(Long id);
}
