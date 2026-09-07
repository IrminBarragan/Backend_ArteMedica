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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findByAreaId(Long areaId);

    // Los cuatro metodos siguientes traen medico, area y farmaceutico en la misma consulta.
    // Sin el JOIN FETCH, cada solicitud de la pagina disparaba tres consultas adicionales al
    // leerse esas asociaciones perezosas para armar la respuesta.
    // Solo se enlazan asociaciones a-uno: un JOIN FETCH de coleccion obligaria a Hibernate a
    // paginar en memoria despues de traerlo todo.

    @Query(value = "SELECT s FROM Solicitud s JOIN FETCH s.medico JOIN FETCH s.area "
            + "LEFT JOIN FETCH s.farmaceutico",
           countQuery = "SELECT COUNT(s) FROM Solicitud s")
    Page<Solicitud> buscarTodas(Pageable pageable);

    @Query(value = "SELECT s FROM Solicitud s JOIN FETCH s.medico JOIN FETCH s.area "
            + "LEFT JOIN FETCH s.farmaceutico WHERE s.estatus = :estatus",
           countQuery = "SELECT COUNT(s) FROM Solicitud s WHERE s.estatus = :estatus")
    Page<Solicitud> buscarPorEstatus(@Param("estatus") EstatusSolicitud estatus, Pageable pageable);

    @Query(value = "SELECT s FROM Solicitud s JOIN FETCH s.medico m JOIN FETCH s.area "
            + "LEFT JOIN FETCH s.farmaceutico WHERE m.id = :medicoId",
           countQuery = "SELECT COUNT(s) FROM Solicitud s WHERE s.medico.id = :medicoId")
    Page<Solicitud> buscarPorMedico(@Param("medicoId") Long medicoId, Pageable pageable);

    @Query(value = "SELECT s FROM Solicitud s JOIN FETCH s.medico m JOIN FETCH s.area "
            + "LEFT JOIN FETCH s.farmaceutico WHERE s.estatus = :estatus AND m.id = :medicoId",
           countQuery = "SELECT COUNT(s) FROM Solicitud s WHERE s.estatus = :estatus "
                   + "AND s.medico.id = :medicoId")
    Page<Solicitud> buscarPorEstatusYMedico(@Param("estatus") EstatusSolicitud estatus,
                                            @Param("medicoId") Long medicoId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT s FROM Solicitud s WHERE s.id = :id")
    Optional<Solicitud> findByIdForUpdate(@Param("id") Long id);
}
