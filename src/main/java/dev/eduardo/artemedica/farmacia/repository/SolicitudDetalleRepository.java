package dev.eduardo.artemedica.farmacia.repository;

import dev.eduardo.artemedica.farmacia.model.SolicitudDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudDetalleRepository extends JpaRepository<SolicitudDetalle, Long> {
    List<SolicitudDetalle> findBySolicitudId(Long solicitudId);
}
