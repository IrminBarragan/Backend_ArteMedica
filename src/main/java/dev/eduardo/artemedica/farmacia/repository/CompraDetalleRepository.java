package dev.eduardo.artemedica.farmacia.repository;

import dev.eduardo.artemedica.farmacia.model.CompraDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

import java.util.List;

public interface CompraDetalleRepository extends JpaRepository<CompraDetalle, Long> {
    @Query("SELECT d FROM CompraDetalle d JOIN FETCH d.producto LEFT JOIN FETCH d.lote "
            + "WHERE d.compra.id = :compraId")
    List<CompraDetalle> findByCompraId(@Param("compraId") Long compraId);

    @Query("SELECT d FROM CompraDetalle d JOIN FETCH d.producto LEFT JOIN FETCH d.lote "
            + "WHERE d.compra.id IN :compraIds")
    List<CompraDetalle> findByCompraIdIn(@Param("compraIds") Collection<Long> compraIds);
}
