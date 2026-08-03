package dev.eduardo.artemedica.farmacia.repository;

import dev.eduardo.artemedica.farmacia.model.CodigoEquivalente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodigoEquivalenteRepository extends JpaRepository<CodigoEquivalente, Long> {
    Optional<CodigoEquivalente> findByCodigoBarrasAndActivoTrue(String codigoBarras);
    List<CodigoEquivalente> findByProductoId(Long productoId);
}
