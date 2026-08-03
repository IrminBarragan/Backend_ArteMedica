package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.ProveedorRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.ProveedorResponseDTO;

import java.util.List;

public interface ProveedorService {
    ProveedorResponseDTO crear(ProveedorRequestDTO dto);
    ProveedorResponseDTO actualizar(Long id, ProveedorRequestDTO dto);
    ProveedorResponseDTO obtenerPorId(Long id);
    List<ProveedorResponseDTO> listarActivos();
    void desactivar(Long id);
}
