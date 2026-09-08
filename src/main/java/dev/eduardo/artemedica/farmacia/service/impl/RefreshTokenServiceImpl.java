package dev.eduardo.artemedica.farmacia.service.impl;

import dev.eduardo.artemedica.farmacia.exception.RefreshTokenInvalidoException;
import dev.eduardo.artemedica.farmacia.model.RefreshToken;
import dev.eduardo.artemedica.farmacia.model.Usuario;
import dev.eduardo.artemedica.farmacia.repository.RefreshTokenRepository;
import dev.eduardo.artemedica.farmacia.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.refresh.expiration-ms}")
    private long expiracionMs;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public String emitir(Usuario usuario) {
        String tokenPlano = generarTokenAleatorio();
        LocalDateTime ahora = LocalDateTime.now();

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(hash(tokenPlano))
                .usuario(usuario)
                .expiraEn(ahora.plus(Duration.ofMillis(expiracionMs)))
                .createdAt(ahora)
                .build();
        refreshTokenRepository.save(refreshToken);

        return tokenPlano;
    }

    @Override
    @Transactional
    public TokenRotado rotar(String tokenPlano) {
        RefreshToken actual = refreshTokenRepository.findByTokenHash(hash(tokenPlano))
                .orElseThrow(() -> new RefreshTokenInvalidoException("Refresh token invalido."));

        if (actual.getRevocadoEn() != null) {
            // Un token ya rotado o revocado que vuelve a usarse es la senal clasica de que
            // alguien mas tiene una copia: se cierran todas las sesiones del usuario, no solo
            // se rechaza este intento.
            refreshTokenRepository.revocarTodasDeUsuario(actual.getUsuario().getId(), LocalDateTime.now());
            throw new RefreshTokenInvalidoException(
                    "Refresh token ya utilizado. Por seguridad se cerraron todas las sesiones; inicia sesion de nuevo.");
        }
        if (actual.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new RefreshTokenInvalidoException("Refresh token expirado.");
        }

        actual.setRevocadoEn(LocalDateTime.now());
        refreshTokenRepository.save(actual);

        String nuevoToken = emitir(actual.getUsuario());
        return new TokenRotado(actual.getUsuario(), nuevoToken);
    }

    @Override
    @Transactional
    public void revocar(String tokenPlano) {
        refreshTokenRepository.findByTokenHash(hash(tokenPlano)).ifPresent(refreshToken -> {
            refreshToken.setRevocadoEn(LocalDateTime.now());
            refreshTokenRepository.save(refreshToken);
        });
    }

    @Override
    public long getExpiracionMs() {
        return expiracionMs;
    }

    private String generarTokenAleatorio() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en vez de BCrypt: no es una contrasena que haya que resistir fuerza bruta con
     * costo intencional, es un valor de 64 bytes generado con SecureRandom, imposible de
     * adivinar; SHA-256 alcanza y es mucho mas barato de calcular en cada refresh. */
    private String hash(String tokenPlano) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tokenPlano.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
