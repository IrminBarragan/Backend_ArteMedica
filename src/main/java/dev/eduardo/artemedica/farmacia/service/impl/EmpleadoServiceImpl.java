package dev.eduardo.artemedica.farmacia.service.impl;

import dev.eduardo.artemedica.farmacia.dto.EmpleadoRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.EmpleadoResponseDTO;
import dev.eduardo.artemedica.farmacia.exception.ResourceNotFoundException;
import dev.eduardo.artemedica.farmacia.model.Empleado;
import dev.eduardo.artemedica.farmacia.repository.EmpleadoRepository;
import dev.eduardo.artemedica.farmacia.service.EmpleadoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmpleadoServiceImpl implements EmpleadoService {

    private final EmpleadoRepository empleadoRepository;

    public EmpleadoServiceImpl(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    @Override
    @Transactional
    public EmpleadoResponseDTO crear(EmpleadoRequestDTO dto) {
        Empleado empleado = Empleado.builder()
                .nombres(dto.nombres())
                .apellidoPaterno(dto.apellidoPaterno())
                .apellidoMaterno(dto.apellidoMaterno())
                .tipo(dto.tipo())
                .especialidad(dto.especialidad())
                .cedulaProfesional(dto.cedulaProfesional())
                .telefonoGuardia(dto.telefonoGuardia())
                .activo(true)
                .build();
        return toDto(empleadoRepository.save(empleado));
    }

    @Override
    @Transactional
    public EmpleadoResponseDTO actualizar(Long id, EmpleadoRequestDTO dto) {
        Empleado empleado = obtenerEntidad(id);
        empleado.setNombres(dto.nombres());
        empleado.setApellidoPaterno(dto.apellidoPaterno());
        empleado.setApellidoMaterno(dto.apellidoMaterno());
        empleado.setTipo(dto.tipo());
        empleado.setEspecialidad(dto.especialidad());
        empleado.setCedulaProfesional(dto.cedulaProfesional());
        empleado.setTelefonoGuardia(dto.telefonoGuardia());
        return toDto(empleadoRepository.save(empleado));
    }

    @Override
    @Transactional(readOnly = true)
    public EmpleadoResponseDTO obtenerPorId(Long id) {
        return toDto(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmpleadoResponseDTO> listarActivos() {
        return empleadoRepository.findByActivoTrue().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Empleado empleado = obtenerEntidad(id);
        empleado.setActivo(false);
        empleadoRepository.save(empleado);
    }

    private Empleado obtenerEntidad(Long id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + id));
    }

    private EmpleadoResponseDTO toDto(Empleado empleado) {
        return new EmpleadoResponseDTO(
                empleado.getId(), empleado.getNombres(), empleado.getApellidoPaterno(), empleado.getApellidoMaterno(),
                empleado.getTipo(), empleado.getEspecialidad(), empleado.getCedulaProfesional(),
                empleado.getTelefonoGuardia(), empleado.isActivo()
        );
    }
}
