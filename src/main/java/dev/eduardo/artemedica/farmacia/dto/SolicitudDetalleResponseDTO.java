package dev.eduardo.artemedica.farmacia.dto;

public record SolicitudDetalleResponseDTO(
        Long id, Long productoId, String productoNombre, String presentacion,
        Integer cantidadSolicitada, Integer cantidadAutorizada, Integer cantidadEntregada,
        Long loteId, String numeroLote
) {}
