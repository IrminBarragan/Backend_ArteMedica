package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.AjusteRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.MermaRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.MovimientoInventarioResponseDTO;
import dev.eduardo.artemedica.farmacia.exception.ReglaNegocioException;
import dev.eduardo.artemedica.farmacia.exception.StockInsuficienteException;
import dev.eduardo.artemedica.farmacia.model.Lote;
import dev.eduardo.artemedica.farmacia.model.Producto;
import dev.eduardo.artemedica.farmacia.model.Usuario;
import dev.eduardo.artemedica.farmacia.model.enums.TipoMovimiento;
import dev.eduardo.artemedica.farmacia.repository.LoteRepository;
import dev.eduardo.artemedica.farmacia.repository.MovimientoInventarioRepository;
import dev.eduardo.artemedica.farmacia.repository.UsuarioRepository;
import dev.eduardo.artemedica.farmacia.service.impl.MovimientoInventarioServiceImpl;
import dev.eduardo.artemedica.farmacia.service.support.StockAjustador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Mermas, ajustes y baja de lote")
class MovimientoInventarioServiceTest {

    @Mock private MovimientoInventarioRepository movimientoRepository;
    @Mock private LoteRepository loteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private StockAjustador stockAjustador;

    @InjectMocks private MovimientoInventarioServiceImpl service;

    private Producto producto;
    private Lote lote;
    private Usuario usuario;

    @BeforeEach
    void prepararDatos() {
        producto = Producto.builder().id(7L).nombre("Paracetamol").stockActual(100).build();
        lote = Lote.builder().id(3L).numeroLote("PAR-A").producto(producto)
                .existenciaActual(40).activo(true).build();
        usuario = Usuario.builder().id(1L).username("farmacia.demo").build();
    }

    private void mockearGuardado() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(loteRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(lote));
        when(movimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("una merma descuenta del lote, del stock del producto y queda en el kardex")
    void mermaDescuentaLoteYStock() {
        mockearGuardado();

        MovimientoInventarioResponseDTO resultado =
                service.registrarMerma(new MermaRequestDTO(3L, 10, "Frasco roto"), 1L);

        assertThat(lote.getExistenciaActual()).isEqualTo(30);
        verify(stockAjustador).ajustarStock(eq(7L), eq(-10));
        assertThat(resultado.tipoMovimiento()).isEqualTo(TipoMovimiento.MERMA);
        assertThat(resultado.cantidad()).isEqualTo(10);
        assertThat(resultado.saldoResultante()).isEqualTo(30);
        assertThat(resultado.motivo()).isEqualTo("Frasco roto");
    }

    @Test
    @DisplayName("no se puede mermar mas de lo que queda en el lote")
    void mermaMayorQueExistenciaFalla() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(loteRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(lote));

        assertThatThrownBy(() -> service.registrarMerma(new MermaRequestDTO(3L, 41, "Caducado"), 1L))
                .isInstanceOf(StockInsuficienteException.class);

        assertThat(lote.getExistenciaActual()).isEqualTo(40);
        verify(stockAjustador, never()).ajustarStock(any(), anyInt());
    }

    @Test
    @DisplayName("un ajuste positivo suma al lote y al stock")
    void ajustePositivoSuma() {
        mockearGuardado();

        MovimientoInventarioResponseDTO resultado =
                service.registrarAjuste(new AjusteRequestDTO(3L, 5, "Conteo fisico"), 1L);

        assertThat(lote.getExistenciaActual()).isEqualTo(45);
        verify(stockAjustador).ajustarStock(eq(7L), eq(5));
        assertThat(resultado.tipoMovimiento()).isEqualTo(TipoMovimiento.ENTRADA);
    }

    @Test
    @DisplayName("un ajuste negativo resta del lote y del stock")
    void ajusteNegativoResta() {
        mockearGuardado();

        MovimientoInventarioResponseDTO resultado =
                service.registrarAjuste(new AjusteRequestDTO(3L, -15, "Conteo fisico"), 1L);

        assertThat(lote.getExistenciaActual()).isEqualTo(25);
        verify(stockAjustador).ajustarStock(eq(7L), eq(-15));
        assertThat(resultado.tipoMovimiento()).isEqualTo(TipoMovimiento.SALIDA);
    }

    @Test
    @DisplayName("un ajuste de cero se rechaza")
    void ajusteCeroFalla() {
        assertThatThrownBy(() -> service.registrarAjuste(new AjusteRequestDTO(3L, 0, "Nada"), 1L))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("un ajuste negativo no puede dejar el lote en negativo")
    void ajusteNegativoExcesivoFalla() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(loteRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(lote));

        assertThatThrownBy(() -> service.registrarAjuste(new AjusteRequestDTO(3L, -41, "Error"), 1L))
                .isInstanceOf(StockInsuficienteException.class);

        assertThat(lote.getExistenciaActual()).isEqualTo(40);
    }

    @Test
    @DisplayName("dar de baja un lote merma lo que quedaba: el stock del producto NO queda inflado")
    void bajaDeLoteDescuentaElStockRestante() {
        mockearGuardado();

        service.darDeBajaLote(3L, "Caducado", 1L);

        // Este es el bug que se corrige: antes el lote se marcaba inactivo pero sus 40 unidades
        // seguian contando en Producto.stockActual para siempre.
        assertThat(lote.isActivo()).isFalse();
        assertThat(lote.getExistenciaActual()).isZero();
        verify(stockAjustador).ajustarStock(eq(7L), eq(-40));

        ArgumentCaptor<dev.eduardo.artemedica.farmacia.model.MovimientoInventario> captor =
                ArgumentCaptor.forClass(dev.eduardo.artemedica.farmacia.model.MovimientoInventario.class);
        verify(movimientoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipoMovimiento()).isEqualTo(TipoMovimiento.MERMA);
        assertThat(captor.getValue().getMotivo()).isEqualTo("Caducado");
    }

    @Test
    @DisplayName("dar de baja un lote ya vacio no toca el stock pero si lo desactiva")
    void bajaDeLoteVacioNoTocaStock() {
        lote.setExistenciaActual(0);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(loteRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(lote));

        service.darDeBajaLote(3L, "Agotado y caducado", 1L);

        assertThat(lote.isActivo()).isFalse();
        verify(stockAjustador, never()).ajustarStock(any(), anyInt());
    }

    @Test
    @DisplayName("no se puede dar de baja dos veces el mismo lote")
    void bajaDobleFalla() {
        lote.setActivo(false);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(loteRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(lote));

        assertThatThrownBy(() -> service.darDeBajaLote(3L, "Caducado", 1L))
                .isInstanceOf(ReglaNegocioException.class);
    }
}
