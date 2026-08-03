package dev.eduardo.artemedica.farmacia.service.impl;

import dev.eduardo.artemedica.farmacia.dto.LoteResponseDTO;
import dev.eduardo.artemedica.farmacia.exception.ResourceNotFoundException;
import dev.eduardo.artemedica.farmacia.model.Lote;
import dev.eduardo.artemedica.farmacia.repository.LoteRepository;
import dev.eduardo.artemedica.farmacia.service.LoteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LoteServiceImpl implements LoteService {

    private final LoteRepository loteRepository;

    public LoteServiceImpl(LoteRepository loteRepository) {
        this.loteRepository = loteRepository;
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
    public void desactivar(Long id) {
        Lote lote = obtenerEntidad(id);
        lote.setActivo(false);
        loteRepository.save(lote);
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
