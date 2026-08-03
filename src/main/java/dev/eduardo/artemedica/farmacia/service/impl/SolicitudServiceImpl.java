package dev.eduardo.artemedica.farmacia.service.impl;

import dev.eduardo.artemedica.farmacia.dto.SolicitudDetalleRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.SolicitudDetalleResponseDTO;
import dev.eduardo.artemedica.farmacia.dto.SolicitudRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.SolicitudResponseDTO;
import dev.eduardo.artemedica.farmacia.exception.ConflictoConcurrenciaException;
import dev.eduardo.artemedica.farmacia.exception.EstadoInvalidoException;
import dev.eduardo.artemedica.farmacia.exception.ResourceNotFoundException;
import dev.eduardo.artemedica.farmacia.exception.StockInsuficienteException;
import dev.eduardo.artemedica.farmacia.model.Area;
import dev.eduardo.artemedica.farmacia.model.Empleado;
import dev.eduardo.artemedica.farmacia.model.Lote;
import dev.eduardo.artemedica.farmacia.model.MovimientoInventario;
import dev.eduardo.artemedica.farmacia.model.Producto;
import dev.eduardo.artemedica.farmacia.model.Solicitud;
import dev.eduardo.artemedica.farmacia.model.SolicitudDetalle;
import dev.eduardo.artemedica.farmacia.model.Usuario;
import dev.eduardo.artemedica.farmacia.model.enums.EstatusSolicitud;
import dev.eduardo.artemedica.farmacia.model.enums.OrigenMovimiento;
import dev.eduardo.artemedica.farmacia.model.enums.TipoMovimiento;
import dev.eduardo.artemedica.farmacia.repository.AreaRepository;
import dev.eduardo.artemedica.farmacia.repository.EmpleadoRepository;
import dev.eduardo.artemedica.farmacia.repository.LoteRepository;
import dev.eduardo.artemedica.farmacia.repository.MovimientoInventarioRepository;
import dev.eduardo.artemedica.farmacia.repository.ProductoRepository;
import dev.eduardo.artemedica.farmacia.repository.SolicitudDetalleRepository;
import dev.eduardo.artemedica.farmacia.repository.SolicitudRepository;
import dev.eduardo.artemedica.farmacia.repository.UsuarioRepository;
import dev.eduardo.artemedica.farmacia.service.SolicitudService;
import dev.eduardo.artemedica.farmacia.service.strategy.LoteSeleccionStrategyResolver;
import dev.eduardo.artemedica.farmacia.service.support.StockAjustador;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class SolicitudServiceImpl implements SolicitudService {

    private static final int MAX_INTENTOS_STOCK = 3;

    private final SolicitudRepository solicitudRepository;
    private final SolicitudDetalleRepository solicitudDetalleRepository;
    private final EmpleadoRepository empleadoRepository;
    private final AreaRepository areaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteRepository loteRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final LoteSeleccionStrategyResolver loteSeleccionStrategyResolver;
    private final StockAjustador stockAjustador;

    public SolicitudServiceImpl(SolicitudRepository solicitudRepository,
                                 SolicitudDetalleRepository solicitudDetalleRepository,
                                 EmpleadoRepository empleadoRepository,
                                 AreaRepository areaRepository,
                                 ProductoRepository productoRepository,
                                 UsuarioRepository usuarioRepository,
                                 LoteRepository loteRepository,
                                 MovimientoInventarioRepository movimientoInventarioRepository,
                                 LoteSeleccionStrategyResolver loteSeleccionStrategyResolver,
                                 StockAjustador stockAjustador) {
        this.solicitudRepository = solicitudRepository;
        this.solicitudDetalleRepository = solicitudDetalleRepository;
        this.empleadoRepository = empleadoRepository;
        this.areaRepository = areaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.loteRepository = loteRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.loteSeleccionStrategyResolver = loteSeleccionStrategyResolver;
        this.stockAjustador = stockAjustador;
    }

    @Override
    @Transactional
    public SolicitudResponseDTO crear(SolicitudRequestDTO dto, Long medicoId) {
        Empleado medico = empleadoRepository.findById(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado (medico) no encontrado: " + medicoId));
        Area area = areaRepository.findById(dto.areaId())
                .orElseThrow(() -> new ResourceNotFoundException("Area no encontrada: " + dto.areaId()));

        LocalDateTime ahora = LocalDateTime.now();
        Solicitud solicitud = Solicitud.builder()
                .medico(medico)
                .area(area)
                .fechaSolicitud(ahora)
                .estatus(EstatusSolicitud.PENDIENTE)
                .createdAt(ahora)
                .build();
        solicitud = solicitudRepository.save(solicitud);

        List<SolicitudDetalle> detalles = new ArrayList<>();
        for (SolicitudDetalleRequestDTO detalleDto : dto.detalles()) {
            Producto producto = productoRepository.findById(detalleDto.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + detalleDto.productoId()));
            SolicitudDetalle detalle = SolicitudDetalle.builder()
                    .solicitud(solicitud)
                    .producto(producto)
                    .cantidadSolicitada(detalleDto.cantidadSolicitada())
                    .cantidadEntregada(0)
                    .build();
            detalles.add(solicitudDetalleRepository.save(detalle));
        }

        return toDto(solicitud, detalles);
    }

    @Override
    @Transactional
    public SolicitudResponseDTO aprobar(Long solicitudId, Map<Long, Integer> cantidadesAutorizadasPorProducto, Long farmaceuticoId) {
        return conLockManejado(() -> {
            Solicitud solicitud = solicitudRepository.findByIdForUpdate(solicitudId)
                    .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada: " + solicitudId));
            if (solicitud.getEstatus() != EstatusSolicitud.PENDIENTE) {
                throw new EstadoInvalidoException("La solicitud " + solicitudId + " no esta en estatus PENDIENTE.");
            }
            Empleado farmaceutico = empleadoRepository.findById(farmaceuticoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Empleado (farmaceutico) no encontrado: " + farmaceuticoId));

            List<SolicitudDetalle> detalles = solicitudDetalleRepository.findBySolicitudId(solicitudId);
            LocalDate hoy = LocalDate.now();
            for (SolicitudDetalle detalle : detalles) {
                Producto producto = detalle.getProducto();
                int cantidadAutorizada = cantidadesAutorizadasPorProducto.getOrDefault(producto.getId(), 0);

                int disponible = loteRepository.findByProductoIdAndActivoTrueAndExistenciaActualGreaterThan(producto.getId(), 0)
                        .stream()
                        .filter(lote -> !lote.getFechaCaducidad().isBefore(hoy))
                        .mapToInt(Lote::getExistenciaActual)
                        .sum();

                if (cantidadAutorizada > disponible) {
                    throw new StockInsuficienteException(
                            "Stock insuficiente para " + producto.getNombre() + ": disponible " + disponible + ", solicitado " + cantidadAutorizada);
                }

                detalle.setCantidadAutorizada(cantidadAutorizada);
                solicitudDetalleRepository.save(detalle);
            }

            solicitud.setEstatus(EstatusSolicitud.APROBADO);
            solicitud.setFarmaceutico(farmaceutico);
            solicitud.setFechaAprobacion(LocalDateTime.now());
            Solicitud actualizada = solicitudRepository.save(solicitud);

            return toDto(actualizada, detalles);
        });
    }

    @Override
    @Transactional
    public SolicitudResponseDTO rechazar(Long solicitudId, String motivo, Long farmaceuticoId) {
        return conLockManejado(() -> {
            Solicitud solicitud = solicitudRepository.findByIdForUpdate(solicitudId)
                    .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada: " + solicitudId));
            if (solicitud.getEstatus() != EstatusSolicitud.PENDIENTE) {
                throw new EstadoInvalidoException("La solicitud " + solicitudId + " no esta en estatus PENDIENTE.");
            }
            Empleado farmaceutico = empleadoRepository.findById(farmaceuticoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Empleado (farmaceutico) no encontrado: " + farmaceuticoId));

            solicitud.setEstatus(EstatusSolicitud.RECHAZADO);
            solicitud.setMotivoRechazo(motivo);
            solicitud.setFarmaceutico(farmaceutico);
            solicitud.setFechaAprobacion(LocalDateTime.now());
            Solicitud actualizada = solicitudRepository.save(solicitud);

            List<SolicitudDetalle> detalles = solicitudDetalleRepository.findBySolicitudId(solicitudId);
            return toDto(actualizada, detalles);
        });
    }

    @Override
    @Transactional
    public SolicitudResponseDTO dispensar(Long solicitudId, Long farmaceuticoId) {
        return conLockManejado(() -> {
            Solicitud solicitud = solicitudRepository.findByIdForUpdate(solicitudId)
                    .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada: " + solicitudId));
            if (solicitud.getEstatus() != EstatusSolicitud.APROBADO
                    && solicitud.getEstatus() != EstatusSolicitud.ENTREGADA_PARCIAL) {
                throw new EstadoInvalidoException(
                        "La solicitud " + solicitudId + " no esta en estatus APROBADO ni ENTREGADA_PARCIAL.");
            }
            if (!empleadoRepository.existsById(farmaceuticoId)) {
                throw new ResourceNotFoundException("Empleado (farmaceutico) no encontrado: " + farmaceuticoId);
            }
            Usuario usuarioFarmaceutico = usuarioRepository.findByEmpleadoId(farmaceuticoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado para el empleado: " + farmaceuticoId));

            List<SolicitudDetalle> detalles = solicitudDetalleRepository.findBySolicitudId(solicitudId);
            LocalDateTime ahora = LocalDateTime.now();

            for (SolicitudDetalle detalle : detalles) {
                int autorizada = detalle.getCantidadAutorizada() == null ? 0 : detalle.getCantidadAutorizada();
                int entregada = detalle.getCantidadEntregada() == null ? 0 : detalle.getCantidadEntregada();
                int pendiente = autorizada - entregada;
                if (pendiente <= 0) {
                    continue;
                }

                Long productoId = detalle.getProducto().getId();
                List<Lote> lotesDisponibles = loteSeleccionStrategyResolver.resolver()
                        .obtenerLotesDisponiblesParaConsumo(productoId);

                for (Lote lote : lotesDisponibles) {
                    if (pendiente <= 0) {
                        break;
                    }
                    int cantidadTomada = Math.min(pendiente, lote.getExistenciaActual());
                    if (cantidadTomada <= 0) {
                        continue;
                    }

                    lote.setExistenciaActual(lote.getExistenciaActual() - cantidadTomada);
                    lote.setUpdatedAt(ahora);
                    loteRepository.save(lote);

                    MovimientoInventario movimiento = MovimientoInventario.builder()
                            .lote(lote)
                            .producto(detalle.getProducto())
                            .tipoMovimiento(TipoMovimiento.SALIDA)
                            .cantidad(cantidadTomada)
                            .saldoResultante(lote.getExistenciaActual())
                            .usuario(usuarioFarmaceutico)
                            .fechaMovimiento(ahora)
                            .origenTipo(OrigenMovimiento.SOLICITUD)
                            .origenId(solicitud.getId())
                            .build();
                    movimientoInventarioRepository.save(movimiento);

                    pendiente -= cantidadTomada;
                    entregada += cantidadTomada;
                    detalle.setCantidadEntregada(entregada);
                    if (detalle.getLote() == null) {
                        detalle.setLote(lote);
                    }

                    stockAjustador.actualizarStockConReintento(productoId, -cantidadTomada, MAX_INTENTOS_STOCK);
                }

                solicitudDetalleRepository.save(detalle);
            }

            boolean todosCompletos = detalles.stream().allMatch(this::estaCompleto);
            boolean algunaEntrega = detalles.stream()
                    .anyMatch(d -> d.getCantidadEntregada() != null && d.getCantidadEntregada() > 0);

            if (todosCompletos) {
                solicitud.setEstatus(EstatusSolicitud.ENTREGADA_COMPLETA);
                solicitud.setFechaEntrega(ahora);
            } else if (algunaEntrega) {
                solicitud.setEstatus(EstatusSolicitud.ENTREGADA_PARCIAL);
            }
            Solicitud actualizada = solicitudRepository.save(solicitud);

            return toDto(actualizada, detalles);
        });
    }

    private boolean estaCompleto(SolicitudDetalle detalle) {
        int autorizada = detalle.getCantidadAutorizada() == null ? 0 : detalle.getCantidadAutorizada();
        int entregada = detalle.getCantidadEntregada() == null ? 0 : detalle.getCantidadEntregada();
        return entregada >= autorizada;
    }

    private <T> T conLockManejado(Supplier<T> accion) {
        try {
            return accion.get();
        } catch (PessimisticLockException | LockTimeoutException | PessimisticLockingFailureException e) {
            throw new ConflictoConcurrenciaException(
                    "Otro usuario esta procesando este mismo lote/solicitud en este momento, intenta de nuevo en unos segundos.");
        }
    }

    private SolicitudResponseDTO toDto(Solicitud solicitud, List<SolicitudDetalle> detalles) {
        Empleado medico = solicitud.getMedico();
        Empleado farmaceutico = solicitud.getFarmaceutico();
        List<SolicitudDetalleResponseDTO> detallesDto = detalles.stream()
                .map(this::toDetalleDto)
                .toList();

        return new SolicitudResponseDTO(
                solicitud.getId(), medico.getId(), medico.getNombres() + " " + medico.getApellidoPaterno(),
                solicitud.getArea().getId(), solicitud.getArea().getNombre(),
                solicitud.getFechaSolicitud(), solicitud.getEstatus(),
                farmaceutico != null ? farmaceutico.getNombres() + " " + farmaceutico.getApellidoPaterno() : null,
                solicitud.getFechaAprobacion(), solicitud.getFechaEntrega(), solicitud.getMotivoRechazo(),
                detallesDto, solicitud.getCreatedAt()
        );
    }

    private SolicitudDetalleResponseDTO toDetalleDto(SolicitudDetalle detalle) {
        Producto producto = detalle.getProducto();
        Lote lote = detalle.getLote();
        return new SolicitudDetalleResponseDTO(
                detalle.getId(), producto.getId(), producto.getNombre(), producto.getPresentacion(),
                detalle.getCantidadSolicitada(), detalle.getCantidadAutorizada(), detalle.getCantidadEntregada(),
                lote != null ? lote.getId() : null, lote != null ? lote.getNumeroLote() : null
        );
    }
}
