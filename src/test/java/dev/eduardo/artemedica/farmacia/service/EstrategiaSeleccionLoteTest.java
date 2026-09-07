package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.model.CategoriaMedicamento;
import dev.eduardo.artemedica.farmacia.model.ConfiguracionSistema;
import dev.eduardo.artemedica.farmacia.model.Lote;
import dev.eduardo.artemedica.farmacia.model.Producto;
import dev.eduardo.artemedica.farmacia.model.Proveedor;
import dev.eduardo.artemedica.farmacia.repository.CategoriaMedicamentoRepository;
import dev.eduardo.artemedica.farmacia.repository.ConfiguracionSistemaRepository;
import dev.eduardo.artemedica.farmacia.repository.LoteRepository;
import dev.eduardo.artemedica.farmacia.repository.ProductoRepository;
import dev.eduardo.artemedica.farmacia.repository.ProveedorRepository;
import dev.eduardo.artemedica.farmacia.service.strategy.LoteSeleccionStrategyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El interruptor FEFO/FIFO es la pieza central del modulo de inventario: decide de que lote
 * sale el medicamento cuando un producto tiene varios disponibles.
 *
 * Los dos lotes de estas pruebas tienen orden de entrada y orden de caducidad opuestos, que es
 * la unica forma de distinguir una estrategia de la otra: si el lote mas antiguo fuera tambien
 * el que caduca antes, ambas darian el mismo resultado y la prueba no probaria nada.
 */
@SpringBootTest
@Transactional
@DisplayName("Seleccion de lote FEFO / FIFO")
class EstrategiaSeleccionLoteTest {

    private static final String CLAVE_FEFO = "INVENTARIO_FEFO_HABILITADO";

    @Autowired private LoteSeleccionStrategyResolver resolver;
    @Autowired private LoteRepository loteRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private CategoriaMedicamentoRepository categoriaRepository;
    @Autowired private ConfiguracionSistemaRepository configuracionRepository;

    private Long productoId;

    @BeforeEach
    void crearDosLotesConOrdenOpuesto() {
        CategoriaMedicamento categoria = categoriaRepository.save(
                CategoriaMedicamento.builder().nombre("Analgesicos").activo(true).build());
        Proveedor proveedor = proveedorRepository.save(
                Proveedor.builder().nombre("Distribuidora Norte").activo(true).build());
        Producto producto = productoRepository.save(Producto.builder()
                .nombre("Paracetamol").presentacion("Tabletas").categoria(categoria)
                .precioVenta(BigDecimal.ONE).precioCompra(BigDecimal.ONE)
                .stockMinimo(0).stockActual(140).activo(true).build());
        productoId = producto.getId();

        LocalDateTime ahora = LocalDateTime.now();

        // ANTIGUO: entro primero, pero caduca dentro de mucho.
        loteRepository.save(Lote.builder()
                .numeroLote("ANTIGUO").producto(producto).proveedor(proveedor)
                .fechaCaducidad(LocalDate.now().plusMonths(18))
                .costoCompra(BigDecimal.ONE).cantidadInicial(100).existenciaActual(100)
                .activo(true).createdAt(ahora.minusDays(30)).build());

        // RECIENTE: entro despues, pero caduca pronto.
        loteRepository.save(Lote.builder()
                .numeroLote("RECIENTE").producto(producto).proveedor(proveedor)
                .fechaCaducidad(LocalDate.now().plusMonths(3))
                .costoCompra(BigDecimal.ONE).cantidadInicial(40).existenciaActual(40)
                .activo(true).createdAt(ahora.minusDays(5)).build());
    }

    private void configurarFefo(boolean habilitado) {
        ConfiguracionSistema config = configuracionRepository.findByClave(CLAVE_FEFO)
                .orElseGet(() -> ConfiguracionSistema.builder().clave(CLAVE_FEFO).build());
        config.setValor(String.valueOf(habilitado));
        config.setUpdatedAt(LocalDateTime.now());
        config.setUpdatedBy("test");
        configuracionRepository.save(config);
    }

    @Test
    @DisplayName("con FEFO sale primero el lote que caduca antes, aunque haya entrado despues")
    void fefoPrefiereElQueCaducaAntes() {
        configurarFefo(true);

        List<Lote> orden = resolver.resolver().obtenerLotesDisponiblesParaConsumo(productoId);

        assertThat(orden).extracting(Lote::getNumeroLote)
                .containsExactly("RECIENTE", "ANTIGUO");
    }

    @Test
    @DisplayName("con FIFO sale primero el lote que entro antes, aunque caduque despues")
    void fifoPrefiereElQueEntroAntes() {
        configurarFefo(false);

        List<Lote> orden = resolver.resolver().obtenerLotesDisponiblesParaConsumo(productoId);

        assertThat(orden).extracting(Lote::getNumeroLote)
                .containsExactly("ANTIGUO", "RECIENTE");
    }

    @Test
    @DisplayName("si la clave de configuracion no existe, se asume FEFO")
    void sinConfiguracionUsaFefo() {
        configuracionRepository.findByClave(CLAVE_FEFO).ifPresent(configuracionRepository::delete);

        List<Lote> orden = resolver.resolver().obtenerLotesDisponiblesParaConsumo(productoId);

        // FEFO es la practica estandar en farmacia: ante la duda, minimizar medicamento caducado.
        assertThat(orden).extracting(Lote::getNumeroLote).containsExactly("RECIENTE", "ANTIGUO");
    }

    @Test
    @DisplayName("ninguna estrategia ofrece lotes ya caducados")
    void ningunaEstrategiaOfreceCaducados() {
        loteRepository.findByProductoIdAndActivoTrue(productoId, Pageable.unpaged()).forEach(lote -> {
            lote.setFechaCaducidad(LocalDate.now().minusDays(1));
            loteRepository.save(lote);
        });

        configurarFefo(true);
        assertThat(resolver.resolver().obtenerLotesDisponiblesParaConsumo(productoId)).isEmpty();

        configurarFefo(false);
        assertThat(resolver.resolver().obtenerLotesDisponiblesParaConsumo(productoId)).isEmpty();
    }

    @Test
    @DisplayName("ninguna estrategia ofrece lotes agotados ni dados de baja")
    void ningunaEstrategiaOfreceAgotadosNiInactivos() {
        List<Lote> lotes = loteRepository.findByProductoIdAndActivoTrue(productoId, Pageable.unpaged()).getContent();
        lotes.stream().filter(l -> l.getNumeroLote().equals("RECIENTE")).forEach(l -> {
            l.setExistenciaActual(0);
            loteRepository.save(l);
        });
        lotes.stream().filter(l -> l.getNumeroLote().equals("ANTIGUO")).forEach(l -> {
            l.setActivo(false);
            loteRepository.save(l);
        });

        configurarFefo(true);
        assertThat(resolver.resolver().obtenerLotesDisponiblesParaConsumo(productoId)).isEmpty();
    }

    @Test
    @DisplayName("cambiar la configuracion cambia la estrategia sin reiniciar la aplicacion")
    void elCambioDeConfiguracionSurteEfectoDeInmediato() {
        configurarFefo(true);
        assertThat(resolver.resolver().obtenerLotesDisponiblesParaConsumo(productoId).getFirst()
                .getNumeroLote()).isEqualTo("RECIENTE");

        configurarFefo(false);
        assertThat(resolver.resolver().obtenerLotesDisponiblesParaConsumo(productoId).getFirst()
                .getNumeroLote()).isEqualTo("ANTIGUO");
    }
}
