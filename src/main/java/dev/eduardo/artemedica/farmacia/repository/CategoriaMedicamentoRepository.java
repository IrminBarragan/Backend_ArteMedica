package dev.eduardo.artemedica.farmacia.repository;

import dev.eduardo.artemedica.farmacia.model.CategoriaMedicamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaMedicamentoRepository extends JpaRepository<CategoriaMedicamento, Long> {
    List<CategoriaMedicamento> findByActivoTrue();
}
