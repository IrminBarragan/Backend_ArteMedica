package dev.eduardo.artemedica.farmacia.controller;

import dev.eduardo.artemedica.farmacia.dto.AprobarSolicitudRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.RechazarSolicitudRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.SolicitudRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.SolicitudResponseDTO;
import dev.eduardo.artemedica.farmacia.model.enums.EstatusSolicitud;
import dev.eduardo.artemedica.farmacia.model.enums.Rol;
import dev.eduardo.artemedica.farmacia.security.UsuarioPrincipal;
import dev.eduardo.artemedica.farmacia.service.SolicitudService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<SolicitudResponseDTO> crear(@Valid @RequestBody SolicitudRequestDTO dto,
                                                        @AuthenticationPrincipal UsuarioPrincipal principal) {
        SolicitudResponseDTO creada = solicitudService.crear(dto, principal.getEmpleadoId());
        return ResponseEntity.created(URI.create("/api/solicitudes/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('FARMACEUTICO') or hasRole('ADMIN')")
    public ResponseEntity<SolicitudResponseDTO> aprobar(@PathVariable Long id,
                                                          @Valid @RequestBody AprobarSolicitudRequestDTO dto,
                                                          @AuthenticationPrincipal UsuarioPrincipal principal) {
        SolicitudResponseDTO actualizada = solicitudService.aprobar(
                id, dto.cantidadesAutorizadasPorProducto(), principal.getEmpleadoId());
        return ResponseEntity.ok(actualizada);
    }

    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('FARMACEUTICO') or hasRole('ADMIN')")
    public ResponseEntity<SolicitudResponseDTO> rechazar(@PathVariable Long id,
                                                           @Valid @RequestBody RechazarSolicitudRequestDTO dto,
                                                           @AuthenticationPrincipal UsuarioPrincipal principal) {
        SolicitudResponseDTO actualizada = solicitudService.rechazar(id, dto.motivo(), principal.getEmpleadoId());
        return ResponseEntity.ok(actualizada);
    }

    @PutMapping("/{id}/dispensar")
    @PreAuthorize("hasRole('FARMACEUTICO') or hasRole('ADMIN')")
    public ResponseEntity<SolicitudResponseDTO> dispensar(@PathVariable Long id,
                                                            @AuthenticationPrincipal UsuarioPrincipal principal) {
        SolicitudResponseDTO actualizada = solicitudService.dispensar(id, principal.getEmpleadoId());
        return ResponseEntity.ok(actualizada);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<SolicitudResponseDTO>> listar(@RequestParam(required = false) EstatusSolicitud estatus,
                                                               @AuthenticationPrincipal UsuarioPrincipal principal) {
        Long medicoId = principal.getRol() == Rol.MEDICO ? principal.getEmpleadoId() : null;
        return ResponseEntity.ok(solicitudService.listar(estatus, medicoId));
    }
}
