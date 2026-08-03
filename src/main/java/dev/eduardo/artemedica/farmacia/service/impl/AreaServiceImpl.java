package dev.eduardo.artemedica.farmacia.service.impl;

import dev.eduardo.artemedica.farmacia.dto.AreaRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.AreaResponseDTO;
import dev.eduardo.artemedica.farmacia.exception.ResourceNotFoundException;
import dev.eduardo.artemedica.farmacia.model.Area;
import dev.eduardo.artemedica.farmacia.repository.AreaRepository;
import dev.eduardo.artemedica.farmacia.service.AreaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AreaServiceImpl implements AreaService {

    private final AreaRepository areaRepository;

    public AreaServiceImpl(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    @Override
    @Transactional
    public AreaResponseDTO crear(AreaRequestDTO dto) {
        Area area = Area.builder()
                .nombre(dto.nombre())
                .descripcion(dto.descripcion())
                .activo(true)
                .build();
        return toDto(areaRepository.save(area));
    }

    @Override
    @Transactional
    public AreaResponseDTO actualizar(Long id, AreaRequestDTO dto) {
        Area area = obtenerEntidad(id);
        area.setNombre(dto.nombre());
        area.setDescripcion(dto.descripcion());
        return toDto(areaRepository.save(area));
    }

    @Override
    @Transactional(readOnly = true)
    public AreaResponseDTO obtenerPorId(Long id) {
        return toDto(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaResponseDTO> listarActivos() {
        return areaRepository.findByActivoTrue().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Area area = obtenerEntidad(id);
        area.setActivo(false);
        areaRepository.save(area);
    }

    private Area obtenerEntidad(Long id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Area no encontrada: " + id));
    }

    private AreaResponseDTO toDto(Area area) {
        return new AreaResponseDTO(area.getId(), area.getNombre(), area.getDescripcion(), area.isActivo());
    }
}
