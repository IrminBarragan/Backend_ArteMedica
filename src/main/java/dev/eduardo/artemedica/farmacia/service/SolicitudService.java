package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.SolicitudRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.SolicitudResponseDTO;

import java.util.Map;

public interface SolicitudService {
    SolicitudResponseDTO crear(SolicitudRequestDTO dto, Long medicoId);
    SolicitudResponseDTO aprobar(Long solicitudId, Map<Long, Integer> cantidadesAutorizadasPorProducto, Long farmaceuticoId);
    SolicitudResponseDTO rechazar(Long solicitudId, String motivo, Long farmaceuticoId);
    SolicitudResponseDTO dispensar(Long solicitudId, Long farmaceuticoId);
}
