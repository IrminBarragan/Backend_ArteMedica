package dev.eduardo.artemedica.farmacia.controller;

import dev.eduardo.artemedica.farmacia.dto.LoginRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.LoginResponseDTO;
import dev.eduardo.artemedica.farmacia.security.JwtService;
import dev.eduardo.artemedica.farmacia.security.UsuarioDetailsService;
import dev.eduardo.artemedica.farmacia.security.UsuarioPrincipal;
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

    public AuthController(UsuarioService usuarioService,
                           UsuarioDetailsService usuarioDetailsService,
                           JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.usuarioDetailsService = usuarioDetailsService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        usuarioService.autenticar(dto.username(), dto.password());
        UsuarioPrincipal principal = (UsuarioPrincipal) usuarioDetailsService.loadUserByUsername(dto.username());
        String token = jwtService.generarToken(principal);

        LoginResponseDTO response = new LoginResponseDTO(
                token, "Bearer", principal.getUsername(), principal.getRol(),
                principal.getEmpleadoId(), jwtService.getExpirationMs()
        );
        return ResponseEntity.ok(response);
    }
}
