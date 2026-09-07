package dev.eduardo.artemedica.farmacia.repository;

import dev.eduardo.artemedica.farmacia.model.Compra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    // Un mismo folio puede repetirse entre proveedores distintos, pero no dentro del mismo.
    boolean existsByProveedorIdAndNumeroFactura(Long proveedorId, String numeroFactura);

    // Proveedor y usuario de registro viajan en la misma consulta: se leen para cada fila
    // al armar la respuesta y provocaban dos consultas extra por compra.

    @Query(value = "SELECT c FROM Compra c JOIN FETCH c.proveedor JOIN FETCH c.usuarioRegistro",
           countQuery = "SELECT COUNT(c) FROM Compra c")
    Page<Compra> buscarTodas(Pageable pageable);

    @Query(value = "SELECT c FROM Compra c JOIN FETCH c.proveedor p JOIN FETCH c.usuarioRegistro "
            + "WHERE p.id = :proveedorId",
           countQuery = "SELECT COUNT(c) FROM Compra c WHERE c.proveedor.id = :proveedorId")
    Page<Compra> buscarPorProveedor(@Param("proveedorId") Long proveedorId, Pageable pageable);

    @Query(value = "SELECT c FROM Compra c JOIN FETCH c.proveedor JOIN FETCH c.usuarioRegistro "
            + "WHERE c.fechaCompra BETWEEN :desde AND :hasta",
           countQuery = "SELECT COUNT(c) FROM Compra c WHERE c.fechaCompra BETWEEN :desde AND :hasta")
    Page<Compra> buscarPorRangoDeFechas(@Param("desde") LocalDate desde,
                                        @Param("hasta") LocalDate hasta, Pageable pageable);
}
