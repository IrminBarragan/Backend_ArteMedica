package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.PaginaDTO;
import dev.eduardo.artemedica.farmacia.dto.ProductoRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.ProductoResponseDTO;
import org.springframework.data.domain.Pageable;

public interface ProductoService {
    ProductoResponseDTO crear(ProductoRequestDTO dto);
    ProductoResponseDTO actualizar(Long id, ProductoRequestDTO dto);
    ProductoResponseDTO obtenerPorId(Long id);
    PaginaDTO<ProductoResponseDTO> listarActivos(Pageable pageable);
    PaginaDTO<ProductoResponseDTO> listarStockBajo(Pageable pageable);
    void desactivar(Long id);
}
