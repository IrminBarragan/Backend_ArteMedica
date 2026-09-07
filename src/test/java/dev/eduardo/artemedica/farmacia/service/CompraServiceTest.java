package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.CompraDetalleRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.CompraRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.CompraResponseDTO;
import dev.eduardo.artemedica.farmacia.exception.ReglaNegocioException;
import dev.eduardo.artemedica.farmacia.model.Compra;
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
import dev.eduardo.artemedica.farmacia.service.impl.CompraServiceImpl;
import dev.eduardo.artemedica.farmacia.service.support.StockAjustador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Registro de compras")
class CompraServiceTest {

    @Mock private ProveedorRepository proveedorRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private CompraRepository compraRepository;
    @Mock private CompraDetalleRepository compraDetalleRepository;
    @Mock private LoteRepository loteRepository;
    @Mock private MovimientoInventarioRepository movimientoInventarioRepository;
    @Mock private StockAjustador stockAjustador;

    @InjectMocks private CompraServiceImpl service;

    private Proveedor proveedor;
    private Producto paracetamol;
    private Usuario usuario;

    @BeforeEach
    void prepararDatos() {
        proveedor = Proveedor.builder().id(1L).nombre("Distribuidora Norte").activo(true).build();
        paracetamol = Producto.builder().id(2L).nombre("Paracetamol").stockActual(0).activo(true).build();
        usuario = Usuario.builder().id(3L).username("farmacia.demo").build();
    }

    private CompraRequestDTO compraDe(int cantidad, BigDecimal costo) {
        return new CompraRequestDTO(1L, "FAC-001", LocalDate.now().minusDays(1),
                List.of(new CompraDetalleRequestDTO(2L, "LOTE-A",
                        LocalDate.now().plusMonths(12), cantidad, costo)));
    }

    private void mockearCompraValida() {
        lenient().when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        lenient().when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario));
        lenient().when(productoRepository.findById(2L)).thenReturn(Optional.of(paracetamol));
        lenient().when(compraRepository.existsByProveedorIdAndNumeroFactura(anyLong(), anyString()))
                .thenReturn(false);
        lenient().when(compraRepository.save(any())).thenAnswer(inv -> {
            Compra c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });
        lenient().when(loteRepository.save(any())).thenAnswer(inv -> {
            Lote l = inv.getArgument(0);
            l.setId(20L);
            return l;
        });
        lenient().when(compraDetalleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(movimientoInventarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("una compra crea el lote, suma el stock y deja la entrada en el kardex")
    void compraCreaLoteStockYKardex() {
        mockearCompraValida();

        CompraResponseDTO resultado = service.registrarCompra(compraDe(100, new BigDecimal("1.20")), 3L);

        ArgumentCaptor<Lote> loteCaptor = ArgumentCaptor.forClass(Lote.class);
        verify(loteRepository).save(loteCaptor.capture());
        assertThat(loteCaptor.getValue().getCantidadInicial()).isEqualTo(100);
        assertThat(loteCaptor.getValue().getExistenciaActual()).isEqualTo(100);

        verify(stockAjustador).ajustarStock(eq(2L), eq(100));

        ArgumentCaptor<MovimientoInventario> movCaptor =
                ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioRepository).save(movCaptor.capture());
        assertThat(movCaptor.getValue().getTipoMovimiento()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(movCaptor.getValue().getOrigenTipo()).isEqualTo(OrigenMovimiento.COMPRA);
        assertThat(movCaptor.getValue().getOrigenId()).isEqualTo(10L);

        assertThat(resultado.total()).isEqualByComparingTo(new BigDecimal("120.00"));
    }

    @Test
    @DisplayName("el total se calcula en BigDecimal sin perder centavos")
    void totalSinErrorDeRedondeo() {
        mockearCompraValida();

        CompraResponseDTO resultado = service.registrarCompra(compraDe(3, new BigDecimal("0.10")), 3L);

        // Con double, 3 * 0.10 daria 0.30000000000000004.
        assertThat(resultado.total()).isEqualByComparingTo(new BigDecimal("0.30"));
    }

    @Test
    @DisplayName("no se puede registrar dos veces la misma factura del mismo proveedor")
    void facturaDuplicadaFalla() {
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(compraRepository.existsByProveedorIdAndNumeroFactura(1L, "FAC-001")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarCompra(compraDe(10, BigDecimal.ONE), 3L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Ya existe una compra registrada");

        verify(loteRepository, never()).save(any());
        verify(stockAjustador, never()).ajustarStock(any(), anyInt());
    }

    @Test
    @DisplayName("no se puede comprar un producto dado de baja")
    void productoInactivoFalla() {
        paracetamol.setActivo(false);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(2L)).thenReturn(Optional.of(paracetamol));
        when(compraRepository.existsByProveedorIdAndNumeroFactura(anyLong(), anyString())).thenReturn(false);
        when(compraRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.registrarCompra(compraDe(10, BigDecimal.ONE), 3L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no admite entradas de inventario");

        verify(loteRepository, never()).save(any());
    }

    @Test
    @DisplayName("no se puede comprar a un proveedor dado de baja")
    void proveedorInactivoFalla() {
        proveedor.setActivo(false);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));

        assertThatThrownBy(() -> service.registrarCompra(compraDe(10, BigDecimal.ONE), 3L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("dado de baja");

        verify(compraRepository, never()).save(any());
    }

    @Test
    @DisplayName("la misma factura si vale para proveedores distintos")
    void mismaFacturaOtroProveedorEsValida() {
        mockearCompraValida();

        service.registrarCompra(compraDe(10, BigDecimal.ONE), 3L);

        // La unicidad se comprueba por pareja proveedor + folio, no solo por folio.
        verify(compraRepository).existsByProveedorIdAndNumeroFactura(1L, "FAC-001");
    }
}
