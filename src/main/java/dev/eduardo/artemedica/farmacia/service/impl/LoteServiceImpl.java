package dev.eduardo.artemedica.farmacia.service.impl;

import dev.eduardo.artemedica.farmacia.dto.LoteResponseDTO;
import dev.eduardo.artemedica.farmacia.exception.ResourceNotFoundException;
import dev.eduardo.artemedica.farmacia.model.Lote;
import dev.eduardo.artemedica.farmacia.repository.LoteRepository;
import dev.eduardo.artemedica.farmacia.service.LoteService;
import dev.eduardo.artemedica.farmacia.service.MovimientoInventarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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
    public List<LoteResponseDTO> listarActivos() {
        return loteRepository.findByActivoTrue().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteResponseDTO> listarPorProducto(Long productoId) {
        return loteRepository.findByProductoIdAndActivoTrue(productoId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteResponseDTO> listarVencidos() {
        return loteRepository.findLotesVencidos().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteResponseDTO> listarPorVencer(LocalDate fechaLimite) {
        return loteRepository.findLotesPorVencer(fechaLimite).stream().map(this::toDto).toList();
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
