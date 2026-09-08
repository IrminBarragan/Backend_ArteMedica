package dev.eduardo.artemedica.farmacia.service.impl;

import dev.eduardo.artemedica.farmacia.dto.LoteResponseDTO;
import dev.eduardo.artemedica.farmacia.dto.PaginaDTO;
import dev.eduardo.artemedica.farmacia.exception.ResourceNotFoundException;
import dev.eduardo.artemedica.farmacia.model.Lote;
import dev.eduardo.artemedica.farmacia.repository.LoteRepository;
import dev.eduardo.artemedica.farmacia.service.LoteService;
import dev.eduardo.artemedica.farmacia.service.MovimientoInventarioService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class LoteServiceImpl implements LoteService {

    private final LoteRepository loteRepository;
    private final MovimientoInventarioService movimientoInventarioService;

    public LoteServiceImpl(LoteRepository loteRepository,
                           MovimientoInventarioService movimientoInventarioService) {
        this.loteRepository = loteRepository;
        this.movimientoInventarioService = movimientoInventarioService;
    }

    @Override
    @Transactional(readOnly = true)
    public LoteResponseDTO obtenerPorId(Long id) {
        return toDto(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaDTO<LoteResponseDTO> listarActivos(Pageable pageable) {
        return PaginaDTO.de(loteRepository.findByActivoTrue(pageable), this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaDTO<LoteResponseDTO> listarPorProducto(Long productoId, Pageable pageable) {
        return PaginaDTO.de(loteRepository.findByProductoIdAndActivoTrue(productoId, pageable), this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaDTO<LoteResponseDTO> listarVencidos(Pageable pageable) {
        return PaginaDTO.de(loteRepository.findLotesVencidos(pageable), this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaDTO<LoteResponseDTO> listarPorVencer(LocalDate fechaLimite, Pageable pageable) {
        return PaginaDTO.de(loteRepository.findLotesPorVencer(fechaLimite, pageable), this::toDto);
    }

    @Override
    @Transactional
    public void darDeBaja(Long id, String motivo, Long usuarioId) {
        // La baja se delega en el kardex: alli se merma la existencia restante, se descuenta
        // el stock del producto y queda registrado quien dio de baja el lote y por que.
        movimientoInventarioService.darDeBajaLote(id, motivo, usuarioId);
    }

    private Lote obtenerEntidad(Long id) {
        return loteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado: " + id));
    }

    private LoteResponseDTO toDto(Lote lote) {
        return new LoteResponseDTO(
                lote.getId(), lote.getNumeroLote(), lote.getProducto().getId(), lote.getProducto().getNombre(),
                lote.getProveedor().getId(), lote.getProveedor().getNombre(), lote.getFechaCaducidad(),
                lote.getCostoCompra(), lote.getCantidadInicial(), lote.getExistenciaActual(), lote.isActivo()
        );
    }
}
