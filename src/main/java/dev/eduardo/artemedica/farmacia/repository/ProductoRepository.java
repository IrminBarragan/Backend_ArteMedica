package dev.eduardo.artemedica.farmacia.repository;

import dev.eduardo.artemedica.farmacia.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Page<Producto> findByActivoTrue(Pageable pageable);
    Optional<Producto> findByCodigoBarras(String codigoBarras);

    @Query("SELECT p FROM Producto p WHERE p.activo = true AND p.stockActual <= p.stockMinimo")
    Page<Producto> findProductosStockBajo(Pageable pageable);

    /**
     * Suma delta al contador de stock en una sola sentencia atomica, sin leerlo antes.
     *
     * La base de datos serializa los incrementos concurrentes, asi que dos dispensaciones
     * simultaneas no pueden perder una resta: no hay ventana entre la lectura y la escritura.
     * La condicion del WHERE impide dejar el stock en negativo.
     *
     * @return 1 si se actualizo, 0 si el producto no existe o el resultado seria negativo
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Producto p SET p.stockActual = p.stockActual + :delta "
            + "WHERE p.id = :id AND p.stockActual + :delta >= 0")
    int ajustarStock(@Param("id") Long id, @Param("delta") int delta);
}
