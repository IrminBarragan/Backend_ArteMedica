package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.CodigoEquivalenteRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.CodigoEquivalenteResponseDTO;
import dev.eduardo.artemedica.farmacia.dto.PaginaDTO;
import org.springframework.data.domain.Pageable;

public interface CodigoEquivalenteService {
    CodigoEquivalenteResponseDTO crear(CodigoEquivalenteRequestDTO dto, String createdBy);
    CodigoEquivalenteResponseDTO obtenerPorId(Long id);
    PaginaDTO<CodigoEquivalenteResponseDTO> listarPorProducto(Long productoId, Pageable pageable);
    void desactivar(Long id);
}
