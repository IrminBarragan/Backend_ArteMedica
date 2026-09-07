package dev.eduardo.artemedica.farmacia.repository;

import dev.eduardo.artemedica.farmacia.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    // Un mismo folio puede repetirse entre proveedores distintos, pero no dentro del mismo.
    boolean existsByProveedorIdAndNumeroFactura(Long proveedorId, String numeroFactura);
    List<Compra> findByProveedorId(Long proveedorId);
    List<Compra> findByFechaCompraBetween(LocalDate inicio, LocalDate fin);
}
