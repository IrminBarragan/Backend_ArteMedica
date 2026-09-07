package dev.eduardo.artemedica.farmacia.service.impl;

import dev.eduardo.artemedica.farmacia.dto.AjusteRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.MermaRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.MovimientoInventarioResponseDTO;
import dev.eduardo.artemedica.farmacia.exception.ReglaNegocioException;
import dev.eduardo.artemedica.farmacia.exception.ResourceNotFoundException;
import dev.eduardo.artemedica.farmacia.exception.StockInsuficienteException;
import dev.eduardo.artemedica.farmacia.model.Lote;
import dev.eduardo.artemedica.farmacia.model.MovimientoInventario;
import dev.eduardo.artemedica.farmacia.model.Producto;
import dev.eduardo.artemedica.farmacia.model.Usuario;
import dev.eduardo.artemedica.farmacia.model.enums.OrigenMovimiento;
import dev.eduardo.artemedica.farmacia.model.enums.TipoMovimiento;
import dev.eduardo.artemedica.farmacia.repository.LoteRepository;
import dev.eduardo.artemedica.farmacia.repository.MovimientoInventarioRepository;
import dev.eduardo.artemedica.farmacia.repository.UsuarioRepository;
import dev.eduardo.artemedica.farmacia.service.MovimientoInventarioService;
import dev.eduardo.artemedica.farmacia.service.support.StockAjustador;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimientoInventarioServiceImpl implements MovimientoInventarioService {


    private final MovimientoInventarioRepository movimientoRepository;
    private final LoteRepository loteRepository;
    private final UsuarioRepository usuarioRepository;
    private final StockAjustador stockAjustador;

    public MovimientoInventarioServiceImpl(MovimientoInventarioRepository movimientoRepository,
                                           LoteRepository loteRepository,
                                           UsuarioRepository usuarioRepository,
                                           StockAjustador stockAjustador) {
        this.movimientoRepository = movimientoRepository;
        this.loteRepository = loteRepository;
        this.usuarioRepository = usuarioRepository;
        this.stockAjustador = stockAjustador;
    }

    // ---------------------------------------------------------------------
    // Consulta del kardex
    // ---------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponseDTO> listarPorProducto(Long productoId) {
        return movimientoRepository.findByProductoIdOrderByFechaMovimientoDesc(productoId)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponseDTO> listarPorLote(Long loteId) {
        return movimientoRepository.findByLoteIdOrderByFechaMovimientoDesc(loteId)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponseDTO> listarRecientes() {
        return movimientoRepository.findTop5ByOrderByFechaMovimientoDesc()
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoInventarioResponseDTO> listarPorOrigen(OrigenMovimiento origenTipo, Long origenId) {
        return movimientoRepository.findByOrigenTipoAndOrigenId(origenTipo, origenId)
                .stream().map(this::toDto).toList();
    }

    // ---------------------------------------------------------------------
    // Registro de movimientos manuales
    // ---------------------------------------------------------------------

    @Override
    @Transactional
    public MovimientoInventarioResponseDTO registrarMerma(MermaRequestDTO dto, Long usuarioId) {
        Usuario usuario = obtenerUsuario(usuarioId);
        Lote lote = obtenerLoteBloqueado(dto.loteId());

        if (dto.cantidad() > lote.getExistenciaActual()) {
            throw new StockInsuficienteException("No se puede mermar " + dto.cantidad()
                    + " unidades del lote " + lote.getNumeroLote()
                    + ": solo quedan " + lote.getExistenciaActual() + ".");
        }

        return aplicarSalida(lote, usuario, dto.cantidad(), TipoMovimiento.MERMA, dto.motivo());
    }

    @Override
    @Transactional
    public MovimientoInventarioResponseDTO registrarAjuste(AjusteRequestDTO dto, Long usuarioId) {
        if (dto.cantidad() == 0) {
            throw new ReglaNegocioException("La cantidad de un ajuste no puede ser cero.");
        }

        Usuario usuario = obtenerUsuario(usuarioId);
        Lote lote = obtenerLoteBloqueado(dto.loteId());

        if (dto.cantidad() < 0 && Math.abs(dto.cantidad()) > lote.getExistenciaActual()) {
            throw new StockInsuficienteException("El ajuste dejaria el lote " + lote.getNumeroLote()
                    + " en negativo: existencia actual " + lote.getExistenciaActual()
                    + ", ajuste " + dto.cantidad() + ".");
        }

        return dto.cantidad() > 0
                ? aplicarEntrada(lote, usuario, dto.cantidad(), dto.motivo())
                : aplicarSalida(lote, usuario, Math.abs(dto.cantidad()), TipoMovimiento.SALIDA, dto.motivo());
    }

    @Override
    @Transactional
    public List<MovimientoInventarioResponseDTO> darDeBajaLote(Long loteId, String motivo, Long usuarioId) {
        Usuario usuario = obtenerUsuario(usuarioId);
        Lote lote = obtenerLoteBloqueado(loteId);

        if (!lote.isActivo()) {
            throw new ReglaNegocioException("El lote " + lote.getNumeroLote() + " ya esta dado de baja.");
        }

        // Las unidades que aun quedaban salen como merma: si solo se marcara el lote
        // como inactivo, seguirian contando en Producto.stockActual y el stock quedaria inflado.
        List<MovimientoInventarioResponseDTO> movimientos = List.of();
        if (lote.getExistenciaActual() > 0) {
            movimientos = List.of(aplicarSalida(lote, usuario, lote.getExistenciaActual(),
                    TipoMovimiento.MERMA, motivo));
        }

        lote.setActivo(false);
        lote.setUpdatedAt(LocalDateTime.now());
        loteRepository.save(lote);

        return movimientos;
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Descuenta del lote y del stock del producto, y deja constancia en el kardex. */
    private MovimientoInventarioResponseDTO aplicarSalida(Lote lote, Usuario usuario, int cantidad,
                                                          TipoMovimiento tipo, String motivo) {
        LocalDateTime ahora = LocalDateTime.now();
        lote.setExistenciaActual(lote.getExistenciaActual() - cantidad);
        lote.setUpdatedAt(ahora);
        loteRepository.save(lote);

        Producto producto = lote.getProducto();
        stockAjustador.ajustarStock(producto.getId(), -cantidad);

        return toDto(guardarMovimiento(lote, producto, usuario, tipo, cantidad, motivo, ahora));
    }

    /** Suma al lote y al stock del producto, y deja constancia en el kardex. */
    private MovimientoInventarioResponseDTO aplicarEntrada(Lote lote, Usuario usuario, int cantidad, String motivo) {
        LocalDateTime ahora = LocalDateTime.now();
        lote.setExistenciaActual(lote.getExistenciaActual() + cantidad);
        lote.setUpdatedAt(ahora);
        loteRepository.save(lote);

        Producto producto = lote.getProducto();
        stockAjustador.ajustarStock(producto.getId(), cantidad);

        return toDto(guardarMovimiento(lote, producto, usuario, TipoMovimiento.ENTRADA, cantidad, motivo, ahora));
    }

    private MovimientoInventario guardarMovimiento(Lote lote, Producto producto, Usuario usuario,
                                                   TipoMovimiento tipo, int cantidad, String motivo,
                                                   LocalDateTime ahora) {
        return movimientoRepository.save(MovimientoInventario.builder()
                .lote(lote)
                .producto(producto)
                .tipoMovimiento(tipo)
                .cantidad(cantidad)
                .saldoResultante(lote.getExistenciaActual())
                .motivo(motivo)
                .usuario(usuario)
                .fechaMovimiento(ahora)
                .origenTipo(OrigenMovimiento.MANUAL)
                .origenId(null)
                .build());
    }

    private Usuario obtenerUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + usuarioId));
    }

    private Lote obtenerLoteBloqueado(Long loteId) {
        return loteRepository.findByIdForUpdate(loteId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado: " + loteId));
    }

    private MovimientoInventarioResponseDTO toDto(MovimientoInventario m) {
        return new MovimientoInventarioResponseDTO(
                m.getId(), m.getLote().getId(), m.getLote().getNumeroLote(),
                m.getProducto().getId(), m.getProducto().getNombre(),
                m.getTipoMovimiento(), m.getCantidad(), m.getSaldoResultante(), m.getMotivo(),
                m.getUsuario().getUsername(), m.getFechaMovimiento(), m.getOrigenTipo(), m.getOrigenId()
        );
    }
}
