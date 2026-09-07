package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.exception.EstadoInvalidoException;
import dev.eduardo.artemedica.farmacia.exception.ReglaNegocioException;
import dev.eduardo.artemedica.farmacia.exception.StockInsuficienteException;
import dev.eduardo.artemedica.farmacia.model.Empleado;
import dev.eduardo.artemedica.farmacia.model.Lote;
import dev.eduardo.artemedica.farmacia.model.Producto;
import dev.eduardo.artemedica.farmacia.model.Solicitud;
import dev.eduardo.artemedica.farmacia.model.SolicitudDetalle;
import dev.eduardo.artemedica.farmacia.model.enums.EstatusSolicitud;
import dev.eduardo.artemedica.farmacia.repository.AreaRepository;
import dev.eduardo.artemedica.farmacia.repository.EmpleadoRepository;
import dev.eduardo.artemedica.farmacia.repository.LoteRepository;
import dev.eduardo.artemedica.farmacia.repository.MovimientoInventarioRepository;
import dev.eduardo.artemedica.farmacia.repository.ProductoRepository;
import dev.eduardo.artemedica.farmacia.repository.SolicitudDetalleRepository;
import dev.eduardo.artemedica.farmacia.repository.SolicitudRepository;
import dev.eduardo.artemedica.farmacia.repository.UsuarioRepository;
import dev.eduardo.artemedica.farmacia.service.impl.SolicitudServiceImpl;
import dev.eduardo.artemedica.farmacia.service.strategy.LoteSeleccionStrategyResolver;
import dev.eduardo.artemedica.farmacia.service.support.StockAjustador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Solicitudes: cancelacion y reglas de aprobacion")
class SolicitudServiceTest {

    @Mock private SolicitudRepository solicitudRepository;
    @Mock private SolicitudDetalleRepository solicitudDetalleRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private AreaRepository areaRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private LoteRepository loteRepository;
    @Mock private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock private LoteSeleccionStrategyResolver loteSeleccionStrategyResolver;
    @Mock private StockAjustador stockAjustador;

    @InjectMocks private SolicitudServiceImpl service;

    private static final Long MEDICO_DUENO = 10L;
    private static final Long OTRO_MEDICO = 99L;
    private static final Long FARMACEUTICO = 20L;

    private Producto paracetamol;
    private Solicitud solicitud;
    private SolicitudDetalle detalle;

    @BeforeEach
    void prepararSolicitudPendiente() {
        Empleado medico = Empleado.builder().id(MEDICO_DUENO).nombres("Laura").apellidoPaterno("Mendoza").build();
        paracetamol = Producto.builder().id(1L).nombre("Paracetamol").presentacion("Tabletas").build();

        solicitud = Solicitud.builder()
                .id(5L).medico(medico)
                .area(dev.eduardo.artemedica.farmacia.model.Area.builder().id(1L).nombre("Urgencias").build())
                .estatus(EstatusSolicitud.PENDIENTE)
                .build();

        detalle = SolicitudDetalle.builder()
                .id(1L).solicitud(solicitud).producto(paracetamol)
                .cantidadSolicitada(30).cantidadEntregada(0).build();
    }

    @Nested
    @DisplayName("Cancelar")
    class Cancelar {

        @Test
        @DisplayName("el medico dueno puede cancelar su solicitud pendiente")
        void duenoCancela() {
            when(solicitudRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(solicitud));
            when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(solicitudDetalleRepository.findBySolicitudId(5L)).thenReturn(List.of(detalle));

            var resultado = service.cancelar(5L, "Me equivoque de paciente", MEDICO_DUENO, false);

            assertThat(resultado.estatus()).isEqualTo(EstatusSolicitud.CANCELADA);
            assertThat(resultado.motivoCancelacion()).isEqualTo("Me equivoque de paciente");
        }

        @Test
        @DisplayName("un medico NO puede cancelar la solicitud de otro medico")
        void medicoAjenoNoCancela() {
            when(solicitudRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(solicitud));

            assertThatThrownBy(() -> service.cancelar(5L, "curioseando", OTRO_MEDICO, false))
                    .isInstanceOf(AccessDeniedException.class);

            assertThat(solicitud.getEstatus()).isEqualTo(EstatusSolicitud.PENDIENTE);
        }

        @Test
        @DisplayName("un ADMIN o FARMACEUTICO si puede cancelar la solicitud de otro")
        void adminCancelaAjena() {
            when(solicitudRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(solicitud));
            when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(solicitudDetalleRepository.findBySolicitudId(5L)).thenReturn(List.of(detalle));

            var resultado = service.cancelar(5L, "Duplicada", FARMACEUTICO, true);

            assertThat(resultado.estatus()).isEqualTo(EstatusSolicitud.CANCELADA);
        }

        @Test
        @DisplayName("no se puede cancelar una solicitud ya aprobada")
        void noCancelaAprobada() {
            solicitud.setEstatus(EstatusSolicitud.APROBADO);
            when(solicitudRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(solicitud));

            assertThatThrownBy(() -> service.cancelar(5L, "tarde", MEDICO_DUENO, false))
                    .isInstanceOf(EstadoInvalidoException.class);
        }
    }

    @Nested
    @DisplayName("Aprobar")
    class Aprobar {

        @BeforeEach
        void mockearAprobacion() {
            lenient().when(solicitudRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(solicitud));
            lenient().when(empleadoRepository.findById(FARMACEUTICO))
                    .thenReturn(Optional.of(Empleado.builder().id(FARMACEUTICO)
                            .nombres("Carlos").apellidoPaterno("Vega").build()));
            lenient().when(solicitudDetalleRepository.findBySolicitudId(5L)).thenReturn(List.of(detalle));
        }

        @Test
        @DisplayName("no se puede autorizar mas de lo que el medico solicito")
        void autorizarDeMasFalla() {
            assertThatThrownBy(() -> service.aprobar(5L, Map.of(1L, 31), FARMACEUTICO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("solicito 30");
        }

        @Test
        @DisplayName("omitir un producto de la solicitud falla en vez de autorizarlo en cero en silencio")
        void omitirProductoFalla() {
            assertThatThrownBy(() -> service.aprobar(5L, Map.of(), FARMACEUTICO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("Falta indicar la cantidad autorizada");
        }

        @Test
        @DisplayName("mandar un producto que no es de la solicitud falla")
        void productoAjenoFalla() {
            assertThatThrownBy(() -> service.aprobar(5L, Map.of(1L, 10, 777L, 5), FARMACEUTICO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("no forman parte de esta solicitud");
        }

        @Test
        @DisplayName("una cantidad negativa falla")
        void cantidadNegativaFalla() {
            assertThatThrownBy(() -> service.aprobar(5L, Map.of(1L, -5), FARMACEUTICO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("no puede ser negativa");
        }

        @Test
        @DisplayName("aprobar menos de lo solicitado es valido y deja la solicitud en APROBADO")
        void aprobacionParcialEsValida() {
            when(loteRepository.findByProductoIdAndActivoTrueAndExistenciaActualGreaterThan(1L, 0))
                    .thenReturn(List.of(Lote.builder().id(3L).existenciaActual(50)
                            .fechaCaducidad(LocalDate.now().plusMonths(6)).build()));
            when(solicitudDetalleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(solicitudRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var resultado = service.aprobar(5L, Map.of(1L, 20), FARMACEUTICO);

            assertThat(resultado.estatus()).isEqualTo(EstatusSolicitud.APROBADO);
            assertThat(resultado.detalles().getFirst().cantidadAutorizada()).isEqualTo(20);
        }

        @Test
        @DisplayName("no se puede aprobar mas de lo que hay disponible en lotes vigentes")
        void sinStockFalla() {
            when(loteRepository.findByProductoIdAndActivoTrueAndExistenciaActualGreaterThan(1L, 0))
                    .thenReturn(List.of(Lote.builder().id(3L).existenciaActual(5)
                            .fechaCaducidad(LocalDate.now().plusMonths(6)).build()));

            assertThatThrownBy(() -> service.aprobar(5L, Map.of(1L, 30), FARMACEUTICO))
                    .isInstanceOf(StockInsuficienteException.class);
        }

        @Test
        @DisplayName("los lotes ya caducados no cuentan como disponibles")
        void loteCaducadoNoCuenta() {
            when(loteRepository.findByProductoIdAndActivoTrueAndExistenciaActualGreaterThan(1L, 0))
                    .thenReturn(List.of(Lote.builder().id(3L).existenciaActual(500)
                            .fechaCaducidad(LocalDate.now().minusDays(1)).build()));

            assertThatThrownBy(() -> service.aprobar(5L, Map.of(1L, 30), FARMACEUTICO))
                    .isInstanceOf(StockInsuficienteException.class)
                    .hasMessageContaining("disponible 0");
        }

        @Test
        @DisplayName("no se puede aprobar una solicitud que no esta pendiente")
        void aprobarNoPendienteFalla() {
            solicitud.setEstatus(EstatusSolicitud.RECHAZADO);

            assertThatThrownBy(() -> service.aprobar(5L, Map.of(1L, 10), FARMACEUTICO))
                    .isInstanceOf(EstadoInvalidoException.class);
        }
    }
}
