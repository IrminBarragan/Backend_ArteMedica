package dev.eduardo.artemedica.farmacia.repository;

import dev.eduardo.artemedica.farmacia.model.CategoriaMedicamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaMedicamentoRepository extends JpaRepository<CategoriaMedicamento, Long> {
    Page<CategoriaMedicamento> findByActivoTrue(Pageable pageable);
}
