package dev.eduardo.artemedica.farmacia.controller;

import dev.eduardo.artemedica.farmacia.dto.BajaLoteRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.LoteResponseDTO;
import dev.eduardo.artemedica.farmacia.dto.PaginaDTO;
import dev.eduardo.artemedica.farmacia.security.UsuarioPrincipal;
import dev.eduardo.artemedica.farmacia.service.LoteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    private final LoteService loteService;

    public LoteController(LoteService loteService) {
        this.loteService = loteService;
    }

    @GetMapping
    public ResponseEntity<PaginaDTO<LoteResponseDTO>> listarActivos(
            @PageableDefault(size = 20, sort = "fechaCaducidad") Pageable pageable) {
        return ResponseEntity.ok(loteService.listarActivos(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoteResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(loteService.obtenerPorId(id));
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<PaginaDTO<LoteResponseDTO>> listarPorProducto(
            @PathVariable Long productoId,
            @PageableDefault(size = 20, sort = "fechaCaducidad") Pageable pageable) {
        return ResponseEntity.ok(loteService.listarPorProducto(productoId, pageable));
    }

    @GetMapping("/vencidos")
    public ResponseEntity<PaginaDTO<LoteResponseDTO>> listarVencidos(
            @PageableDefault(size = 20, sort = "fechaCaducidad") Pageable pageable) {
        return ResponseEntity.ok(loteService.listarVencidos(pageable));
    }

    @GetMapping("/por-vencer")
    public ResponseEntity<PaginaDTO<LoteResponseDTO>> listarPorVencer(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaLimite,
            @PageableDefault(size = 20, sort = "fechaCaducidad") Pageable pageable) {
        return ResponseEntity.ok(loteService.listarPorVencer(fechaLimite, pageable));
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
