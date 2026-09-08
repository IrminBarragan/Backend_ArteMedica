package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.model.Usuario;

/**
 * Refresh tokens opacos (no JWT) guardados hasheados en BD: a diferencia del access token, se
 * pueden revocar de verdad antes de que expiren, lo que permite un logout real.
 */
public interface RefreshTokenService {

    /** Genera y persiste un refresh token nuevo para el usuario. Devuelve el valor en claro. */
    String emitir(Usuario usuario);

    /**
     * Valida el refresh token recibido y lo rota: revoca el actual y emite uno nuevo.
     * Si el token ya estaba revocado (reuso de un token ya rotado), revoca todas las sesiones
     * del usuario por seguridad antes de rechazar la peticion.
     */
    TokenRotado rotar(String tokenPlano);

    /** Revoca el refresh token recibido (logout). No falla si el token no existe o ya estaba revocado. */
    void revocar(String tokenPlano);

    long getExpiracionMs();

    record TokenRotado(Usuario usuario, String token) {}
}
