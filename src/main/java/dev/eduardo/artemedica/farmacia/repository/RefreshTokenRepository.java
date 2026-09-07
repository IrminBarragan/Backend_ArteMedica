package dev.eduardo.artemedica.farmacia.repository;

import dev.eduardo.artemedica.farmacia.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revoca de un golpe todas las sesiones activas de un usuario.
     *
     * Se usa cuando se detecta el reuso de un refresh token ya rotado: es la señal clasica de
     * que alguien mas tiene una copia del token, asi que la respuesta es cerrar todas las
     * sesiones de ese usuario, no solo la que intento reusarlo.
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revocadoEn = :momento WHERE r.usuario.id = :usuarioId AND r.revocadoEn IS NULL")
    int revocarTodasDeUsuario(@Param("usuarioId") Long usuarioId, @Param("momento") LocalDateTime momento);
}
