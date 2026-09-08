package dev.eduardo.artemedica.farmacia.controller;

import dev.eduardo.artemedica.farmacia.dto.CategoriaMedicamentoRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.CategoriaMedicamentoResponseDTO;
import dev.eduardo.artemedica.farmacia.dto.PaginaDTO;
import dev.eduardo.artemedica.farmacia.service.CategoriaMedicamentoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaMedicamentoController {

    private final CategoriaMedicamentoService categoriaMedicamentoService;

    public CategoriaMedicamentoController(CategoriaMedicamentoService categoriaMedicamentoService) {
        this.categoriaMedicamentoService = categoriaMedicamentoService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaMedicamentoResponseDTO> crear(@Valid @RequestBody CategoriaMedicamentoRequestDTO dto) {
        CategoriaMedicamentoResponseDTO creada = categoriaMedicamentoService.crear(dto);
        return ResponseEntity.created(URI.create("/api/categorias/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaMedicamentoResponseDTO> actualizar(@PathVariable Long id,
                                                                       @Valid @RequestBody CategoriaMedicamentoRequestDTO dto) {
        return ResponseEntity.ok(categoriaMedicamentoService.actualizar(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaMedicamentoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaMedicamentoService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<PaginaDTO<CategoriaMedicamentoResponseDTO>> listarActivos(
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return ResponseEntity.ok(categoriaMedicamentoService.listarActivos(pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        categoriaMedicamentoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
