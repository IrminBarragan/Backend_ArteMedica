package dev.eduardo.artemedica.farmacia.controller;

import dev.eduardo.artemedica.farmacia.dto.CodigoEquivalenteRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.CodigoEquivalenteResponseDTO;
import dev.eduardo.artemedica.farmacia.security.UsuarioPrincipal;
import dev.eduardo.artemedica.farmacia.service.CodigoEquivalenteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/codigos-equivalentes")
@PreAuthorize("hasRole('ADMIN') or hasRole('FARMACEUTICO')")
public class CodigoEquivalenteController {

    private final CodigoEquivalenteService codigoEquivalenteService;

    public CodigoEquivalenteController(CodigoEquivalenteService codigoEquivalenteService) {
        this.codigoEquivalenteService = codigoEquivalenteService;
    }

    @PostMapping
    public ResponseEntity<CodigoEquivalenteResponseDTO> crear(@Valid @RequestBody CodigoEquivalenteRequestDTO dto,
                                                                @AuthenticationPrincipal UsuarioPrincipal principal) {
        CodigoEquivalenteResponseDTO creado = codigoEquivalenteService.crear(dto, principal.getUsername());
        return ResponseEntity.created(URI.create("/api/codigos-equivalentes/" + creado.id())).body(creado);
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<CodigoEquivalenteResponseDTO>> listarPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(codigoEquivalenteService.listarPorProducto(productoId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        codigoEquivalenteService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
