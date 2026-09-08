package dev.eduardo.artemedica.farmacia.controller;

import dev.eduardo.artemedica.farmacia.dto.AjusteRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.MermaRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.MovimientoInventarioResponseDTO;
import dev.eduardo.artemedica.farmacia.dto.PaginaDTO;
import dev.eduardo.artemedica.farmacia.model.enums.OrigenMovimiento;
import dev.eduardo.artemedica.farmacia.security.UsuarioPrincipal;
import dev.eduardo.artemedica.farmacia.service.MovimientoInventarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

/**
 * Kardex de inventario: consulta del historico de movimientos y registro de los
 * que no vienen de una compra ni de una dispensacion (mermas y ajustes manuales).
 */
@RestController
@RequestMapping("/api/movimientos")
@PreAuthorize("hasRole('ADMIN') or hasRole('FARMACEUTICO')")
public class MovimientoInventarioController {

    private final MovimientoInventarioService movimientoInventarioService;

    public MovimientoInventarioController(MovimientoInventarioService movimientoInventarioService) {
        this.movimientoInventarioService = movimientoInventarioService;
    }

    @GetMapping("/producto/{productoId}")
    public ResponseEntity<PaginaDTO<MovimientoInventarioResponseDTO>> listarPorProducto(
            @PathVariable Long productoId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(movimientoInventarioService.listarPorProducto(productoId, pageable));
    }

    @GetMapping("/lote/{loteId}")
    public ResponseEntity<PaginaDTO<MovimientoInventarioResponseDTO>> listarPorLote(
            @PathVariable Long loteId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(movimientoInventarioService.listarPorLote(loteId, pageable));
    }

    @GetMapping("/recientes")
    public ResponseEntity<PaginaDTO<MovimientoInventarioResponseDTO>> listarRecientes(
            @PageableDefault(size = 5, sort = "fechaMovimiento", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(movimientoInventarioService.listarRecientes(pageable));
    }

    /** Trazabilidad inversa: todos los movimientos que genero una compra o una solicitud. */
    @GetMapping("/origen")
    public ResponseEntity<PaginaDTO<MovimientoInventarioResponseDTO>> listarPorOrigen(
            @RequestParam OrigenMovimiento origenTipo,
            @RequestParam Long origenId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(movimientoInventarioService.listarPorOrigen(origenTipo, origenId, pageable));
    }

    @PostMapping("/merma")
    public ResponseEntity<MovimientoInventarioResponseDTO> registrarMerma(
            @Valid @RequestBody MermaRequestDTO dto,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(movimientoInventarioService.registrarMerma(dto, principal.getUsuarioId()));
    }

    @PostMapping("/ajuste")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovimientoInventarioResponseDTO> registrarAjuste(
            @Valid @RequestBody AjusteRequestDTO dto,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(movimientoInventarioService.registrarAjuste(dto, principal.getUsuarioId()));
    }
}
