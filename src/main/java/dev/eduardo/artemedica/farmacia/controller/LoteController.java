package dev.eduardo.artemedica.farmacia.controller;

import dev.eduardo.artemedica.farmacia.dto.LoteResponseDTO;
import dev.eduardo.artemedica.farmacia.service.LoteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
