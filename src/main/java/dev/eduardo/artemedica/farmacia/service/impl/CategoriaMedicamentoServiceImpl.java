package dev.eduardo.artemedica.farmacia.service.impl;

import dev.eduardo.artemedica.farmacia.dto.CategoriaMedicamentoRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.CategoriaMedicamentoResponseDTO;
import dev.eduardo.artemedica.farmacia.exception.ResourceNotFoundException;
import dev.eduardo.artemedica.farmacia.model.CategoriaMedicamento;
import dev.eduardo.artemedica.farmacia.repository.CategoriaMedicamentoRepository;
import dev.eduardo.artemedica.farmacia.service.CategoriaMedicamentoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaMedicamentoServiceImpl implements CategoriaMedicamentoService {

    private final CategoriaMedicamentoRepository categoriaMedicamentoRepository;

    public CategoriaMedicamentoServiceImpl(CategoriaMedicamentoRepository categoriaMedicamentoRepository) {
        this.categoriaMedicamentoRepository = categoriaMedicamentoRepository;
    }

    @Override
    @Transactional
    public CategoriaMedicamentoResponseDTO crear(CategoriaMedicamentoRequestDTO dto) {
        CategoriaMedicamento categoria = CategoriaMedicamento.builder()
                .nombre(dto.nombre())
                .descripcion(dto.descripcion())
                .activo(true)
                .build();
        return toDto(categoriaMedicamentoRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaMedicamentoResponseDTO actualizar(Long id, CategoriaMedicamentoRequestDTO dto) {
        CategoriaMedicamento categoria = obtenerEntidad(id);
        categoria.setNombre(dto.nombre());
        categoria.setDescripcion(dto.descripcion());
        return toDto(categoriaMedicamentoRepository.save(categoria));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaMedicamentoResponseDTO obtenerPorId(Long id) {
        return toDto(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaMedicamentoResponseDTO> listarActivos() {
        return categoriaMedicamentoRepository.findByActivoTrue().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        CategoriaMedicamento categoria = obtenerEntidad(id);
        categoria.setActivo(false);
        categoriaMedicamentoRepository.save(categoria);
    }

    private CategoriaMedicamento obtenerEntidad(Long id) {
        return categoriaMedicamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria de medicamento no encontrada: " + id));
    }

    private CategoriaMedicamentoResponseDTO toDto(CategoriaMedicamento categoria) {
        return new CategoriaMedicamentoResponseDTO(
                categoria.getId(), categoria.getNombre(), categoria.getDescripcion(), categoria.isActivo()
        );
    }
}
