package dev.eduardo.artemedica.farmacia.controller;

import dev.eduardo.artemedica.farmacia.dto.AreaRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.AreaResponseDTO;
import dev.eduardo.artemedica.farmacia.service.AreaService;
import jakarta.validation.Valid;
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
import java.util.List;

@RestController
@RequestMapping("/api/areas")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AreaResponseDTO> crear(@Valid @RequestBody AreaRequestDTO dto) {
        AreaResponseDTO creada = areaService.crear(dto);
        return ResponseEntity.created(URI.create("/api/areas/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AreaResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody AreaRequestDTO dto) {
        return ResponseEntity.ok(areaService.actualizar(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AreaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(areaService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<AreaResponseDTO>> listarActivos() {
        return ResponseEntity.ok(areaService.listarActivos());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        areaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
