package dev.eduardo.artemedica.farmacia.repository;

import dev.eduardo.artemedica.farmacia.model.Solicitud;
import dev.eduardo.artemedica.farmacia.model.enums.EstatusSolicitud;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findByEstatus(EstatusSolicitud estatus);
    List<Solicitud> findByMedicoId(Long medicoId);
    List<Solicitud> findByEstatusAndMedicoId(EstatusSolicitud estatus, Long medicoId);
    List<Solicitud> findByAreaId(Long areaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT s FROM Solicitud s WHERE s.id = :id")
    Optional<Solicitud> findByIdForUpdate(@Param("id") Long id);
}
