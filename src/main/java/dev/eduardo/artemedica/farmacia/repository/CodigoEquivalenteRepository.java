package dev.eduardo.artemedica.farmacia.repository;

import dev.eduardo.artemedica.farmacia.model.CodigoEquivalente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodigoEquivalenteRepository extends JpaRepository<CodigoEquivalente, Long> {
    Optional<CodigoEquivalente> findByCodigoBarrasAndActivoTrue(String codigoBarras);
    Page<CodigoEquivalente> findByProductoId(Long productoId, Pageable pageable);
}
