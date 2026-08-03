package dev.eduardo.artemedica.farmacia.service.impl;

import dev.eduardo.artemedica.farmacia.dto.CodigoEquivalenteRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.CodigoEquivalenteResponseDTO;
import dev.eduardo.artemedica.farmacia.exception.ResourceNotFoundException;
import dev.eduardo.artemedica.farmacia.model.CodigoEquivalente;
import dev.eduardo.artemedica.farmacia.model.Producto;
import dev.eduardo.artemedica.farmacia.repository.CodigoEquivalenteRepository;
import dev.eduardo.artemedica.farmacia.repository.ProductoRepository;
import dev.eduardo.artemedica.farmacia.service.CodigoEquivalenteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CodigoEquivalenteServiceImpl implements CodigoEquivalenteService {

    private final CodigoEquivalenteRepository codigoEquivalenteRepository;
    private final ProductoRepository productoRepository;

    public CodigoEquivalenteServiceImpl(CodigoEquivalenteRepository codigoEquivalenteRepository,
                                         ProductoRepository productoRepository) {
        this.codigoEquivalenteRepository = codigoEquivalenteRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    @Transactional
    public CodigoEquivalenteResponseDTO crear(CodigoEquivalenteRequestDTO dto, String createdBy) {
        Producto producto = productoRepository.findById(dto.productoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + dto.productoId()));
        CodigoEquivalente codigoEquivalente = CodigoEquivalente.builder()
                .producto(producto)
                .codigoBarras(dto.codigoBarras())
                .activo(true)
                .createdAt(LocalDateTime.now())
                .createdBy(createdBy)
                .build();
        return toDto(codigoEquivalenteRepository.save(codigoEquivalente));
    }

    @Override
    @Transactional(readOnly = true)
    public CodigoEquivalenteResponseDTO obtenerPorId(Long id) {
        return toDto(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CodigoEquivalenteResponseDTO> listarPorProducto(Long productoId) {
        return codigoEquivalenteRepository.findByProductoId(productoId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        CodigoEquivalente codigoEquivalente = obtenerEntidad(id);
        codigoEquivalente.setActivo(false);
        codigoEquivalenteRepository.save(codigoEquivalente);
    }

    private CodigoEquivalente obtenerEntidad(Long id) {
        return codigoEquivalenteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Codigo equivalente no encontrado: " + id));
    }

    private CodigoEquivalenteResponseDTO toDto(CodigoEquivalente codigoEquivalente) {
        return new CodigoEquivalenteResponseDTO(
                codigoEquivalente.getId(), codigoEquivalente.getProducto().getId(), codigoEquivalente.getProducto().getNombre(),
                codigoEquivalente.getCodigoBarras(), codigoEquivalente.isActivo(),
                codigoEquivalente.getCreatedAt(), codigoEquivalente.getCreatedBy()
        );
    }
}
