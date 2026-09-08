package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.EmpleadoRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.EmpleadoResponseDTO;
import dev.eduardo.artemedica.farmacia.dto.PaginaDTO;
import org.springframework.data.domain.Pageable;

public interface EmpleadoService {
    EmpleadoResponseDTO crear(EmpleadoRequestDTO dto);
    EmpleadoResponseDTO actualizar(Long id, EmpleadoRequestDTO dto);
    EmpleadoResponseDTO obtenerPorId(Long id);
    PaginaDTO<EmpleadoResponseDTO> listarActivos(Pageable pageable);
    void desactivar(Long id);
}
