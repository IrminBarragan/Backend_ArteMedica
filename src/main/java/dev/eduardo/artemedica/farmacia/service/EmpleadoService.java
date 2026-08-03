package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.EmpleadoRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.EmpleadoResponseDTO;

import java.util.List;

public interface EmpleadoService {
    EmpleadoResponseDTO crear(EmpleadoRequestDTO dto);
    EmpleadoResponseDTO actualizar(Long id, EmpleadoRequestDTO dto);
    EmpleadoResponseDTO obtenerPorId(Long id);
    List<EmpleadoResponseDTO> listarActivos();
    void desactivar(Long id);
}
