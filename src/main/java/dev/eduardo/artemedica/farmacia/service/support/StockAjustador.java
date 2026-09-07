package dev.eduardo.artemedica.farmacia.service.support;

import dev.eduardo.artemedica.farmacia.exception.ResourceNotFoundException;
import dev.eduardo.artemedica.farmacia.exception.StockInsuficienteException;
import dev.eduardo.artemedica.farmacia.repository.ProductoRepository;
import org.springframework.stereotype.Component;

/**
 * Unico punto por el que se mueve el contador Producto.stockActual.
 *
 * La version anterior leia el producto, le sumaba el delta y lo guardaba, reintentando hasta
 * tres veces si saltaba un conflicto de bloqueo optimista. Ese reintento no podia funcionar:
 * JPA no ejecuta el UPDATE en el save(), lo difiere hasta el volcado (normalmente en el commit),
 * asi que la excepcion se lanzaba fuera del bloque que la esperaba; y para entonces la
 * transaccion ya estaba marcada para deshacerse, con lo que reintentar dentro no servia de nada.
 *
 * Un contador acumulativo no necesita leerse para actualizarse. Ahora se emite una sola
 * sentencia relativa (stock_actual = stock_actual + delta) que la base de datos serializa por
 * si misma: desaparecen la ventana entre lectura y escritura, el conflicto optimista y el
 * reintento. El @Version de Producto se conserva, pero ya solo protege la edicion concurrente
 * de sus datos de catalogo, no el contador.
 */
@Component
public class StockAjustador {

    private final ProductoRepository productoRepository;

    public StockAjustador(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    /**
     * @param delta unidades a sumar; negativo para restar
     * @throws StockInsuficienteException si la resta dejaria el stock en negativo
     */
    public void ajustarStock(Long productoId, int delta) {
        int filasAfectadas = productoRepository.ajustarStock(productoId, delta);
        if (filasAfectadas == 1) {
            return;
        }

        // No se actualizo ninguna fila: o el producto no existe, o la condicion del WHERE
        // que impide dejar el stock en negativo no se cumplio.
        if (!productoRepository.existsById(productoId)) {
            throw new ResourceNotFoundException("Producto no encontrado: " + productoId);
        }
        throw new StockInsuficienteException(
                "El movimiento dejaria en negativo el stock del producto " + productoId + ".");
    }
}
