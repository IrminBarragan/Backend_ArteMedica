package dev.eduardo.artemedica.farmacia.service.impl;

import dev.eduardo.artemedica.farmacia.dto.CompraDetalleRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.CompraDetalleResponseDTO;
import dev.eduardo.artemedica.farmacia.dto.CompraRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.CompraResponseDTO;
import dev.eduardo.artemedica.farmacia.exception.ResourceNotFoundException;
import dev.eduardo.artemedica.farmacia.model.Compra;
import dev.eduardo.artemedica.farmacia.model.CompraDetalle;
import dev.eduardo.artemedica.farmacia.model.Lote;
import dev.eduardo.artemedica.farmacia.model.MovimientoInventario;
import dev.eduardo.artemedica.farmacia.model.Producto;
import dev.eduardo.artemedica.farmacia.model.Proveedor;
import dev.eduardo.artemedica.farmacia.model.Usuario;
import dev.eduardo.artemedica.farmacia.model.enums.OrigenMovimiento;
import dev.eduardo.artemedica.farmacia.model.enums.TipoMovimiento;
import dev.eduardo.artemedica.farmacia.repository.CompraDetalleRepository;
import dev.eduardo.artemedica.farmacia.repository.CompraRepository;
import dev.eduardo.artemedica.farmacia.repository.LoteRepository;
import dev.eduardo.artemedica.farmacia.repository.MovimientoInventarioRepository;
import dev.eduardo.artemedica.farmacia.repository.ProductoRepository;
import dev.eduardo.artemedica.farmacia.repository.ProveedorRepository;
import dev.eduardo.artemedica.farmacia.repository.UsuarioRepository;
import dev.eduardo.artemedica.farmacia.service.CompraService;
import dev.eduardo.artemedica.farmacia.service.support.StockAjustador;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CompraServiceImpl implements CompraService {

    private static final int MAX_INTENTOS_STOCK = 3;

    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final CompraRepository compraRepository;
    private final CompraDetalleRepository compraDetalleRepository;
    private final LoteRepository loteRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final StockAjustador stockAjustador;

    public CompraServiceImpl(ProveedorRepository proveedorRepository,
                              UsuarioRepository usuarioRepository,
                              ProductoRepository productoRepository,
                              CompraRepository compraRepository,
                              CompraDetalleRepository compraDetalleRepository,
                              LoteRepository loteRepository,
                              MovimientoInventarioRepository movimientoInventarioRepository,
                              StockAjustador stockAjustador) {
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.compraRepository = compraRepository;
        this.compraDetalleRepository = compraDetalleRepository;
        this.loteRepository = loteRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.stockAjustador = stockAjustador;
    }

    @Override
    @Transactional
    public CompraResponseDTO registrarCompra(CompraRequestDTO dto, Long usuarioId) {
        Proveedor proveedor = proveedorRepository.findById(dto.proveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + dto.proveedorId()));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));

        LocalDateTime ahora = LocalDateTime.now();
        Compra compra = Compra.builder()
                .proveedor(proveedor)
                .numeroFactura(dto.numeroFactura())
                .fechaCompra(dto.fechaCompra())
                .usuarioRegistro(usuario)
                .createdAt(ahora)
                .build();
        compra = compraRepository.save(compra);

        List<CompraDetalleResponseDTO> detallesResponse = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CompraDetalleRequestDTO detalleDto : dto.detalles()) {
            Producto producto = productoRepository.findById(detalleDto.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + detalleDto.productoId()));

            CompraDetalle detalle = CompraDetalle.builder()
                    .compra(compra)
                    .producto(producto)
                    .cantidad(detalleDto.cantidad())
                    .costoUnitario(detalleDto.costoUnitario())
                    .build();
            compraDetalleRepository.save(detalle);

            Lote lote = Lote.builder()
                    .numeroLote(detalleDto.numeroLote())
                    .producto(producto)
                    .proveedor(proveedor)
                    .fechaCaducidad(detalleDto.fechaCaducidad())
                    .costoCompra(detalleDto.costoUnitario())
                    .cantidadInicial(detalleDto.cantidad())
                    .existenciaActual(detalleDto.cantidad())
                    .activo(true)
                    .createdAt(ahora)
                    .updatedAt(ahora)
                    .build();
            lote = loteRepository.save(lote);

            stockAjustador.actualizarStockConReintento(producto.getId(), detalleDto.cantidad(), MAX_INTENTOS_STOCK);

            MovimientoInventario movimiento = MovimientoInventario.builder()
                    .lote(lote)
                    .producto(producto)
                    .tipoMovimiento(TipoMovimiento.ENTRADA)
                    .cantidad(detalleDto.cantidad())
                    .saldoResultante(lote.getExistenciaActual())
                    .usuario(usuario)
                    .fechaMovimiento(ahora)
                    .origenTipo(OrigenMovimiento.COMPRA)
                    .origenId(compra.getId())
                    .build();
            movimientoInventarioRepository.save(movimiento);

            BigDecimal subtotal = detalleDto.costoUnitario().multiply(BigDecimal.valueOf(detalleDto.cantidad()));
            total = total.add(subtotal);

            detallesResponse.add(new CompraDetalleResponseDTO(
                    producto.getId(), producto.getNombre(), detalleDto.cantidad(),
                    detalleDto.costoUnitario(), subtotal, lote.getId(), lote.getNumeroLote()
            ));
        }

        return new CompraResponseDTO(
                compra.getId(), proveedor.getId(), proveedor.getNombre(), compra.getNumeroFactura(),
                compra.getFechaCompra(), usuario.getUsername(), total, detallesResponse, compra.getCreatedAt()
        );
    }
}
