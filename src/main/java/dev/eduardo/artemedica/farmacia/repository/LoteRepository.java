package dev.eduardo.artemedica.farmacia.repository;

import dev.eduardo.artemedica.farmacia.model.Lote;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    // Lectura normal, sin bloqueo, para consultas (ej. mostrar disponibilidad en pantalla)
    List<Lote> findByProductoIdAndActivoTrueAndExistenciaActualGreaterThan(Long productoId, Integer cantidad);

    @Query("SELECT l FROM Lote l WHERE l.activo = true AND l.existenciaActual > 0 AND l.fechaCaducidad < CURRENT_DATE")
    List<Lote> findLotesVencidos();

    @Query("SELECT l FROM Lote l WHERE l.activo = true AND l.existenciaActual > 0 AND l.fechaCaducidad BETWEEN CURRENT_DATE AND :fechaLimite")
    List<Lote> findLotesPorVencer(@Param("fechaLimite") LocalDate fechaLimite);

    // --- Con bloqueo pesimista, para usarse SIEMPRE dentro de una transaccion en el Service ---

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT l FROM Lote l WHERE l.id = :id")
    Optional<Lote> findByIdForUpdate(@Param("id") Long id);

    // Orden FEFO: primero el lote que caduca mas pronto
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT l FROM Lote l WHERE l.producto.id = :productoId AND l.activo = true " +
           "AND l.existenciaActual > 0 AND l.fechaCaducidad >= CURRENT_DATE " +
           "ORDER BY l.fechaCaducidad ASC")
    List<Lote> findLotesDisponiblesFefoForUpdate(@Param("productoId") Long productoId);

    // Orden FIFO: primero el lote que entro mas temprano al inventario (fallback si FEFO esta deshabilitado)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT l FROM Lote l WHERE l.producto.id = :productoId AND l.activo = true " +
           "AND l.existenciaActual > 0 AND l.fechaCaducidad >= CURRENT_DATE " +
           "ORDER BY l.createdAt ASC")
    List<Lote> findLotesDisponiblesFifoForUpdate(@Param("productoId") Long productoId);
}
