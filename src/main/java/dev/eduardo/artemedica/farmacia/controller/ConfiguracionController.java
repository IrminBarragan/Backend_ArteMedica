package dev.eduardo.artemedica.farmacia.controller;

import dev.eduardo.artemedica.farmacia.dto.ConfiguracionResponseDTO;
import dev.eduardo.artemedica.farmacia.dto.ConfiguracionValorRequestDTO;
import dev.eduardo.artemedica.farmacia.security.UsuarioPrincipal;
import dev.eduardo.artemedica.farmacia.service.ConfiguracionSistemaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracion")
public class ConfiguracionController {

    private final ConfiguracionSistemaService configuracionSistemaService;

    public ConfiguracionController(ConfiguracionSistemaService configuracionSistemaService) {
        this.configuracionSistemaService = configuracionSistemaService;
    }

    @GetMapping("/{clave}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FARMACEUTICO')")
    public ResponseEntity<ConfiguracionResponseDTO> obtenerPorClave(@PathVariable String clave) {
        return ResponseEntity.ok(configuracionSistemaService.obtenerPorClave(clave));
    }

    @PutMapping("/{clave}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfiguracionResponseDTO> actualizar(@PathVariable String clave,
                                                                 @Valid @RequestBody ConfiguracionValorRequestDTO dto,
                                                                 @AuthenticationPrincipal UsuarioPrincipal principal) {
        ConfiguracionResponseDTO actualizada = configuracionSistemaService.actualizarValor(
                clave, dto.valor(), principal.getUsername());
        return ResponseEntity.ok(actualizada);
    }
}
