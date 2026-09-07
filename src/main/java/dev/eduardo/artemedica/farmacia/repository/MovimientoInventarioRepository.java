package dev.eduardo.artemedica.farmacia.repository;

import dev.eduardo.artemedica.farmacia.model.MovimientoInventario;
import dev.eduardo.artemedica.farmacia.model.enums.OrigenMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    List<MovimientoInventario> findTop5ByOrderByFechaMovimientoDesc();

    // El kardex es la coleccion que mas crece: un movimiento por cada linea de cada compra y
    // de cada dispensacion. Se pagina y se enlazan lote, producto y usuario en la misma
    // consulta, porque los tres se leen para armar cada linea de la respuesta.

    @Query(value = "SELECT m FROM MovimientoInventario m JOIN FETCH m.lote JOIN FETCH m.producto "
            + "JOIN FETCH m.usuario WHERE m.producto.id = :productoId ORDER BY m.fechaMovimiento DESC",
           countQuery = "SELECT COUNT(m) FROM MovimientoInventario m WHERE m.producto.id = :productoId")
    Page<MovimientoInventario> buscarPorProducto(@Param("productoId") Long productoId, Pageable pageable);

    @Query(value = "SELECT m FROM MovimientoInventario m JOIN FETCH m.lote JOIN FETCH m.producto "
            + "JOIN FETCH m.usuario WHERE m.lote.id = :loteId ORDER BY m.fechaMovimiento DESC",
           countQuery = "SELECT COUNT(m) FROM MovimientoInventario m WHERE m.lote.id = :loteId")
    Page<MovimientoInventario> buscarPorLote(@Param("loteId") Long loteId, Pageable pageable);

    @Query("SELECT m FROM MovimientoInventario m JOIN FETCH m.lote JOIN FETCH m.producto "
            + "JOIN FETCH m.usuario WHERE m.origenTipo = :origenTipo AND m.origenId = :origenId "
            + "ORDER BY m.fechaMovimiento DESC")
    List<MovimientoInventario> buscarPorOrigen(@Param("origenTipo") OrigenMovimiento origenTipo,
                                               @Param("origenId") Long origenId);

    @Query("SELECT m FROM MovimientoInventario m JOIN FETCH m.lote JOIN FETCH m.producto "
            + "JOIN FETCH m.usuario ORDER BY m.fechaMovimiento DESC LIMIT 5")
    List<MovimientoInventario> buscarRecientes();
}
