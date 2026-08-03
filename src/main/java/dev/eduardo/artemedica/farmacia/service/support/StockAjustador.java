package dev.eduardo.artemedica.farmacia.service.support;

import dev.eduardo.artemedica.farmacia.exception.ConflictoConcurrenciaException;
import dev.eduardo.artemedica.farmacia.exception.ResourceNotFoundException;
import dev.eduardo.artemedica.farmacia.model.Producto;
import dev.eduardo.artemedica.farmacia.repository.ProductoRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
public class StockAjustador {

    private final ProductoRepository productoRepository;

    public StockAjustador(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public Producto actualizarStockConReintento(Long productoId, int delta, int maxIntentos) {
        int intentos = 0;
        while (true) {
            try {
                Producto producto = productoRepository.findById(productoId)
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
                producto.setStockActual(producto.getStockActual() + delta);
                return productoRepository.save(producto);
            } catch (ObjectOptimisticLockingFailureException e) {
                intentos++;
                if (intentos >= maxIntentos) {
                    throw new ConflictoConcurrenciaException(
                            "No se pudo actualizar el stock del producto " + productoId + " tras " + maxIntentos + " intentos, intenta de nuevo.");
                }
            }
        }
    }
}
