package dev.eduardo.artemedica.farmacia.repository;

import dev.eduardo.artemedica.farmacia.model.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    Page<Proveedor> findByActivoTrue(Pageable pageable);
}
