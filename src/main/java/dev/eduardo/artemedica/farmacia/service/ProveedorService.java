package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.PaginaDTO;
import dev.eduardo.artemedica.farmacia.dto.ProveedorRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.ProveedorResponseDTO;
import org.springframework.data.domain.Pageable;

public interface ProveedorService {
    ProveedorResponseDTO crear(ProveedorRequestDTO dto);
    ProveedorResponseDTO actualizar(Long id, ProveedorRequestDTO dto);
    ProveedorResponseDTO obtenerPorId(Long id);
    PaginaDTO<ProveedorResponseDTO> listarActivos(Pageable pageable);
    void desactivar(Long id);
}
