package dev.eduardo.artemedica.farmacia.dto;

import java.math.BigDecimal;

public record ProductoResponseDTO(
        Long id, String nombre, String presentacion, String codigoBarras, boolean esControlado,
        Long categoriaId, String categoriaNombre, BigDecimal precioVenta, BigDecimal precioCompra,
        Integer stockMinimo, Integer stockActual, boolean activo
) {}
