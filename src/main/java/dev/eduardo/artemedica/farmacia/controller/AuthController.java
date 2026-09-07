package dev.eduardo.artemedica.farmacia.controller;

import dev.eduardo.artemedica.farmacia.dto.LoginRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.LoginResponseDTO;
import dev.eduardo.artemedica.farmacia.dto.LogoutRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.RefreshRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.RefreshResponseDTO;
import dev.eduardo.artemedica.farmacia.security.JwtService;
import dev.eduardo.artemedica.farmacia.security.UsuarioDetailsService;
import dev.eduardo.artemedica.farmacia.security.UsuarioPrincipal;
import dev.eduardo.artemedica.farmacia.service.RefreshTokenService;
import dev.eduardo.artemedica.farmacia.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final UsuarioDetailsService usuarioDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(UsuarioService usuarioService,
                           UsuarioDetailsService usuarioDetailsService,
                           JwtService jwtService,
                           RefreshTokenService refreshTokenService) {
        this.usuarioService = usuarioService;
        this.usuarioDetailsService = usuarioDetailsService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        usuarioService.autenticar(dto.username(), dto.password());
        UsuarioPrincipal principal = (UsuarioPrincipal) usuarioDetailsService.loadUserByUsername(dto.username());
        String token = jwtService.generarToken(principal);
        String refreshToken = refreshTokenService.emitir(principal.getUsuario());

        LoginResponseDTO response = new LoginResponseDTO(
                token, "Bearer", principal.getUsername(), principal.getRol(),
                principal.getEmpleadoId(), jwtService.getExpirationMs(),
                refreshToken, refreshTokenService.getExpiracionMs()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Renueva el access token sin pedir usuario/contrasena de nuevo. El refresh token recibido
     * se rota (se revoca y se emite uno nuevo): el cliente debe descartar el que tenia y
     * guardar el que viene en la respuesta, no reutilizar el anterior.
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO dto) {
        RefreshTokenService.TokenRotado rotado = refreshTokenService.rotar(dto.refreshToken());
        UsuarioPrincipal principal = new UsuarioPrincipal(rotado.usuario());
        String nuevoAccessToken = jwtService.generarToken(principal);

        RefreshResponseDTO response = new RefreshResponseDTO(
                nuevoAccessToken, "Bearer", jwtService.getExpirationMs(),
                rotado.token(), refreshTokenService.getExpiracionMs()
        );
        return ResponseEntity.ok(response);
    }

    /** Revoca el refresh token recibido. El access token en curso sigue siendo valido hasta que expire por si solo. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequestDTO dto) {
        refreshTokenService.revocar(dto.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
