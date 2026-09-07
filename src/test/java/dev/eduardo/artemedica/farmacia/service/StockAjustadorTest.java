package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.exception.StockInsuficienteException;
import dev.eduardo.artemedica.farmacia.model.CategoriaMedicamento;
import dev.eduardo.artemedica.farmacia.model.Producto;
import dev.eduardo.artemedica.farmacia.repository.CategoriaMedicamentoRepository;
import dev.eduardo.artemedica.farmacia.repository.ProductoRepository;
import dev.eduardo.artemedica.farmacia.service.support.StockAjustador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas del contador de stock contra una base de datos real.
 *
 * La concurrencia no se puede verificar con mocks: hace falta que dos transacciones compitan
 * de verdad por la misma fila.
 */
@SpringBootTest
@DisplayName("Contador de stock")
class StockAjustadorTest {

    @Autowired private StockAjustador stockAjustador;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private CategoriaMedicamentoRepository categoriaRepository;
    @Autowired private TransactionTemplate txTemplate;

    private Long productoId;

    @BeforeEach
    void crearProducto() {
        CategoriaMedicamento categoria = categoriaRepository.save(CategoriaMedicamento.builder()
                .nombre("Analgesicos").activo(true).build());
        Producto producto = productoRepository.save(Producto.builder()
                .nombre("Paracetamol").presentacion("Tabletas").categoria(categoria)
                .precioVenta(BigDecimal.ONE).precioCompra(BigDecimal.ONE)
                .stockMinimo(0).stockActual(1000).activo(true).build());
        productoId = producto.getId();
    }

    @Test
    @DisplayName("suma y resta el delta indicado")
    void sumaYResta() {
        txTemplate.executeWithoutResult(tx -> stockAjustador.ajustarStock(productoId, 50));
        assertThat(leerStock()).isEqualTo(1050);

        txTemplate.executeWithoutResult(tx -> stockAjustador.ajustarStock(productoId, -200));
        assertThat(leerStock()).isEqualTo(850);
    }

    @Test
    @DisplayName("rechaza el movimiento que dejaria el stock en negativo")
    void noPermiteNegativo() {
        assertThatThrownBy(() -> txTemplate.executeWithoutResult(
                tx -> stockAjustador.ajustarStock(productoId, -1001)))
                .isInstanceOf(StockInsuficienteException.class);

        assertThat(leerStock()).isEqualTo(1000);
    }

    @Test
    @DisplayName("dejar el stock exactamente en cero si es valido")
    void permiteLlegarACero() {
        txTemplate.executeWithoutResult(tx -> stockAjustador.ajustarStock(productoId, -1000));
        assertThat(leerStock()).isZero();
    }

    @Test
    @DisplayName("veinte descuentos simultaneos no pierden ninguna resta")
    void descuentosConcurrentesNoSePierden() throws InterruptedException {
        int hilos = 20;
        int porHilo = 10;

        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger fallos = new AtomicInteger();

        for (int i = 0; i < hilos; i++) {
            pool.submit(() -> {
                listos.countDown();
                try {
                    salida.await();
                    // Cada hilo abre su propia transaccion y descuenta a la vez que los demas.
                    txTemplate.executeWithoutResult(tx -> stockAjustador.ajustarStock(productoId, -porHilo));
                } catch (Exception e) {
                    fallos.incrementAndGet();
                }
            });
        }

        listos.await(10, TimeUnit.SECONDS);
        salida.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(fallos.get()).as("ningun descuento debio fallar").isZero();
        // Con lectura-modificacion-escritura se perderian actualizaciones y quedaria por encima de 800.
        assertThat(leerStock()).as("20 hilos x 10 unidades = 200 descontadas").isEqualTo(800);
    }

    private int leerStock() {
        return productoRepository.findById(productoId).orElseThrow().getStockActual();
    }
}
