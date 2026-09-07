package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.SolicitudRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.SolicitudResponseDTO;
import dev.eduardo.artemedica.farmacia.model.enums.EstatusSolicitud;

import java.util.List;
import java.util.Map;

public interface SolicitudService {
    SolicitudResponseDTO crear(SolicitudRequestDTO dto, Long medicoId);
    SolicitudResponseDTO aprobar(Long solicitudId, Map<Long, Integer> cantidadesAutorizadasPorProducto, Long farmaceuticoId);
    SolicitudResponseDTO rechazar(Long solicitudId, String motivo, Long farmaceuticoId);
    SolicitudResponseDTO dispensar(Long solicitudId, Long farmaceuticoId);
    /**
     * Retira una solicitud que todavia esta PENDIENTE.
     *
     * @param puedeCancelarAjenas true para ADMIN y FARMACEUTICO; un MEDICO solo puede
     *                            cancelar las solicitudes que el mismo creo.
     */
    SolicitudResponseDTO cancelar(Long solicitudId, String motivo, Long empleadoId, boolean puedeCancelarAjenas);

    /**
     * @param puedeVerAjenas true para ADMIN y FARMACEUTICO; un MEDICO solo puede consultar
     *                       las solicitudes que el mismo creo.
     */
    SolicitudResponseDTO obtenerPorId(Long id, Long empleadoId, boolean puedeVerAjenas);

    // medicoId no nulo restringe el resultado a las solicitudes de ese medico
    List<SolicitudResponseDTO> listar(EstatusSolicitud estatus, Long medicoId);
}
