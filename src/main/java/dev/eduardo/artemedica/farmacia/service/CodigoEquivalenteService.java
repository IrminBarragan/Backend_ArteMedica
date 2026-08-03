package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.CodigoEquivalenteRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.CodigoEquivalenteResponseDTO;

import java.util.List;

public interface CodigoEquivalenteService {
    CodigoEquivalenteResponseDTO crear(CodigoEquivalenteRequestDTO dto, String createdBy);
    CodigoEquivalenteResponseDTO obtenerPorId(Long id);
    List<CodigoEquivalenteResponseDTO> listarPorProducto(Long productoId);
    void desactivar(Long id);
}
