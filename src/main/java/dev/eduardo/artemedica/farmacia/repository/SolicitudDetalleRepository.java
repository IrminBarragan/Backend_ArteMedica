package dev.eduardo.artemedica.farmacia.repository;

import dev.eduardo.artemedica.farmacia.model.SolicitudDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SolicitudDetalleRepository extends JpaRepository<SolicitudDetalle, Long> {
    @Query("SELECT d FROM SolicitudDetalle d JOIN FETCH d.producto LEFT JOIN FETCH d.lote "
            + "WHERE d.solicitud.id = :solicitudId")
    List<SolicitudDetalle> findBySolicitudId(@Param("solicitudId") Long solicitudId);

    /**
     * Trae de una vez los detalles de varias solicitudes, con su producto y su lote.
     *
     * Listar una pagina hacia una consulta de detalles por cada solicitud, mas una por cada
     * producto y lote perezoso de cada linea. Con esto la pagina entera cuesta una sola consulta.
     */
    @Query("SELECT d FROM SolicitudDetalle d JOIN FETCH d.producto LEFT JOIN FETCH d.lote "
            + "WHERE d.solicitud.id IN :solicitudIds")
    List<SolicitudDetalle> findBySolicitudIdIn(@Param("solicitudIds") Collection<Long> solicitudIds);
}
