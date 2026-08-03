package dev.eduardo.artemedica.farmacia.controller;

import dev.eduardo.artemedica.farmacia.dto.ProveedorRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.ProveedorResponseDTO;
import dev.eduardo.artemedica.farmacia.service.ProveedorService;
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
@RequestMapping("/api/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('FARMACEUTICO')")
    public ResponseEntity<ProveedorResponseDTO> crear(@Valid @RequestBody ProveedorRequestDTO dto) {
        ProveedorResponseDTO creado = proveedorService.crear(dto);
        return ResponseEntity.created(URI.create("/api/proveedores/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FARMACEUTICO')")
    public ResponseEntity<ProveedorResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorRequestDTO dto) {
        return ResponseEntity.ok(proveedorService.actualizar(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<ProveedorResponseDTO>> listarActivos() {
        return ResponseEntity.ok(proveedorService.listarActivos());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('FARMACEUTICO')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        proveedorService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
