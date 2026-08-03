package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.ProductoRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.ProductoResponseDTO;

import java.util.List;

public interface ProductoService {
    ProductoResponseDTO crear(ProductoRequestDTO dto);
    ProductoResponseDTO actualizar(Long id, ProductoRequestDTO dto);
    ProductoResponseDTO obtenerPorId(Long id);
    List<ProductoResponseDTO> listarActivos();
    List<ProductoResponseDTO> listarStockBajo();
    void desactivar(Long id);
}
