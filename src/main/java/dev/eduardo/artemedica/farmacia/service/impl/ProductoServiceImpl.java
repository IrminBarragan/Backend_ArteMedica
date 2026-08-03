package dev.eduardo.artemedica.farmacia.service.impl;

import dev.eduardo.artemedica.farmacia.dto.ProductoRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.ProductoResponseDTO;
import dev.eduardo.artemedica.farmacia.exception.ResourceNotFoundException;
import dev.eduardo.artemedica.farmacia.model.CategoriaMedicamento;
import dev.eduardo.artemedica.farmacia.model.Producto;
import dev.eduardo.artemedica.farmacia.repository.CategoriaMedicamentoRepository;
import dev.eduardo.artemedica.farmacia.repository.ProductoRepository;
import dev.eduardo.artemedica.farmacia.service.ProductoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaMedicamentoRepository categoriaMedicamentoRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                                CategoriaMedicamentoRepository categoriaMedicamentoRepository) {
        this.productoRepository = productoRepository;
        this.categoriaMedicamentoRepository = categoriaMedicamentoRepository;
    }

    @Override
    @Transactional
    public ProductoResponseDTO crear(ProductoRequestDTO dto) {
        CategoriaMedicamento categoria = obtenerCategoria(dto.categoriaId());
        Producto producto = Producto.builder()
                .nombre(dto.nombre())
                .presentacion(dto.presentacion())
                .codigoBarras(dto.codigoBarras())
                .esControlado(dto.esControlado())
                .categoria(categoria)
                .precioVenta(dto.precioVenta())
                .precioCompra(dto.precioCompra())
                .stockMinimo(dto.stockMinimo())
                .stockActual(0)
                .activo(true)
                .build();
        return toDto(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponseDTO actualizar(Long id, ProductoRequestDTO dto) {
        Producto producto = obtenerEntidad(id);
        CategoriaMedicamento categoria = obtenerCategoria(dto.categoriaId());
        producto.setNombre(dto.nombre());
        producto.setPresentacion(dto.presentacion());
        producto.setCodigoBarras(dto.codigoBarras());
        producto.setEsControlado(dto.esControlado());
        producto.setCategoria(categoria);
        producto.setPrecioVenta(dto.precioVenta());
        producto.setPrecioCompra(dto.precioCompra());
        producto.setStockMinimo(dto.stockMinimo());
        return toDto(productoRepository.save(producto));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponseDTO obtenerPorId(Long id) {
        return toDto(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> listarActivos() {
        return productoRepository.findByActivoTrue().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> listarStockBajo() {
        return productoRepository.findProductosStockBajo().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Producto producto = obtenerEntidad(id);
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    private CategoriaMedicamento obtenerCategoria(Long categoriaId) {
        return categoriaMedicamentoRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria de medicamento no encontrada: " + categoriaId));
    }

    private Producto obtenerEntidad(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    private ProductoResponseDTO toDto(Producto producto) {
        return new ProductoResponseDTO(
                producto.getId(), producto.getNombre(), producto.getPresentacion(), producto.getCodigoBarras(),
                producto.isEsControlado(), producto.getCategoria().getId(), producto.getCategoria().getNombre(),
                producto.getPrecioVenta(), producto.getPrecioCompra(), producto.getStockMinimo(),
                producto.getStockActual(), producto.isActivo()
        );
    }
}
