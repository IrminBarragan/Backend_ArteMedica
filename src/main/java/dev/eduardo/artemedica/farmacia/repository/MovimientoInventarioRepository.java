package dev.eduardo.artemedica.farmacia.repository;

import dev.eduardo.artemedica.farmacia.model.MovimientoInventario;
import dev.eduardo.artemedica.farmacia.model.enums.OrigenMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    List<MovimientoInventario> findByProductoIdOrderByFechaMovimientoDesc(Long productoId);
    List<MovimientoInventario> findByLoteIdOrderByFechaMovimientoDesc(Long loteId);
    List<MovimientoInventario> findTop5ByOrderByFechaMovimientoDesc();
    List<MovimientoInventario> findByOrigenTipoAndOrigenId(OrigenMovimiento origenTipo, Long origenId);
}
