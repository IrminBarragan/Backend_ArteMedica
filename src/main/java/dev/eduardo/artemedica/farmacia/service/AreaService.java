package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.AreaRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.AreaResponseDTO;

import java.util.List;

public interface AreaService {
    AreaResponseDTO crear(AreaRequestDTO dto);
    AreaResponseDTO actualizar(Long id, AreaRequestDTO dto);
    AreaResponseDTO obtenerPorId(Long id);
    List<AreaResponseDTO> listarActivos();
    void desactivar(Long id);
}
