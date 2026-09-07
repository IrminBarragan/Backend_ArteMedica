package dev.eduardo.artemedica.farmacia.controller;

import dev.eduardo.artemedica.farmacia.dto.BajaLoteRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.LoteResponseDTO;
import dev.eduardo.artemedica.farmacia.security.UsuarioPrincipal;
import dev.eduardo.artemedica.farmacia.service.LoteService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    private final LoteService loteService;

    public LoteController(LoteService loteService) {
        this.loteService = loteService;
    }

    @GetMapping
    public ResponseEntity<List<LoteResponseDTO>> listarActivos() {
        return ResponseEntity.ok(loteService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoteResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(loteService.obtenerPorId(id));
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<LoteResponseDTO>> listarPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(loteService.listarPorProducto(productoId));
    }

    @GetMapping("/vencidos")
    public ResponseEntity<List<LoteResponseDTO>> listarVencidos() {
        return ResponseEntity.ok(loteService.listarVencidos());
    }

    @GetMapping("/por-vencer")
    public ResponseEntity<List<LoteResponseDTO>> listarPorVencer(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaLimite) {
        return ResponseEntity.ok(loteService.listarPorVencer(fechaLimite));
    }

    /**
     * Da de baja el lote completo. No es un DELETE porque no borra nada: merma las unidades
     * restantes, descuenta el stock del producto y deja el rastro en el kardex.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FARMACEUTICO')")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id,
                                          @Valid @RequestBody BajaLoteRequestDTO dto,
                                          @AuthenticationPrincipal UsuarioPrincipal principal) {
        loteService.darDeBaja(id, dto.motivo(), principal.getUsuarioId());
        return ResponseEntity.noContent().build();
    }
}
