package dev.eduardo.artemedica.farmacia.dto;

import dev.eduardo.artemedica.farmacia.model.enums.EstatusSolicitud;

import java.time.LocalDateTime;
import java.util.List;

public record SolicitudResponseDTO(
        Long id, Long medicoId, String medicoNombre, Long areaId, String areaNombre,
        LocalDateTime fechaSolicitud, EstatusSolicitud estatus, String farmaceuticoNombre,
        LocalDateTime fechaAprobacion, LocalDateTime fechaEntrega, String motivoRechazo,
        List<SolicitudDetalleResponseDTO> detalles, LocalDateTime createdAt
) {}
