package dev.eduardo.artemedica.farmacia.service.impl;

import dev.eduardo.artemedica.farmacia.dto.LoginResponseDTO;
import dev.eduardo.artemedica.farmacia.dto.UsuarioRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.UsuarioResponseDTO;
import dev.eduardo.artemedica.farmacia.exception.AutenticacionException;
import dev.eduardo.artemedica.farmacia.exception.ResourceNotFoundException;
import dev.eduardo.artemedica.farmacia.model.Empleado;
import dev.eduardo.artemedica.farmacia.model.Usuario;
import dev.eduardo.artemedica.farmacia.repository.EmpleadoRepository;
import dev.eduardo.artemedica.farmacia.repository.UsuarioRepository;
import dev.eduardo.artemedica.farmacia.service.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                               EmpleadoRepository empleadoRepository,
                               PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.empleadoRepository = empleadoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UsuarioResponseDTO crear(UsuarioRequestDTO dto) {
        Empleado empleado = obtenerEmpleado(dto.empleadoId());
        LocalDateTime ahora = LocalDateTime.now();
        Usuario usuario = Usuario.builder()
                .username(dto.username())
                .password(passwordEncoder.encode(dto.password()))
                .empleado(empleado)
                .rol(dto.rol())
                .activo(true)
                .createdAt(ahora)
                .updatedAt(ahora)
                .build();
        return toDto(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponseDTO actualizar(Long id, UsuarioRequestDTO dto) {
        Usuario usuario = obtenerEntidad(id);
        Empleado empleado = obtenerEmpleado(dto.empleadoId());
        usuario.setUsername(dto.username());
        usuario.setPassword(passwordEncoder.encode(dto.password()));
        usuario.setEmpleado(empleado);
        usuario.setRol(dto.rol());
        usuario.setUpdatedAt(LocalDateTime.now());
        return toDto(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerPorId(Long id) {
        return toDto(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarActivos() {
        return usuarioRepository.findByActivoTrue().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Usuario usuario = obtenerEntidad(id);
        usuario.setActivo(false);
        usuario.setUpdatedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO autenticar(String username, String password) {
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(username)
                .orElseThrow(() -> new AutenticacionException("Usuario o contraseña invalidos"));
        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new AutenticacionException("Usuario o contraseña invalidos");
        }
        return new LoginResponseDTO(usuario.getUsername(), usuario.getRol(), usuario.getEmpleado().getId());
    }

    private Empleado obtenerEmpleado(Long empleadoId) {
        return empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + empleadoId));
    }

    private Usuario obtenerEntidad(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
    }

    private UsuarioResponseDTO toDto(Usuario usuario) {
        Empleado empleado = usuario.getEmpleado();
        return new UsuarioResponseDTO(
                usuario.getId(), usuario.getUsername(), empleado.getId(),
                empleado.getNombres() + " " + empleado.getApellidoPaterno(),
                usuario.getRol(), usuario.isActivo(), usuario.getCreatedAt(), usuario.getUpdatedAt()
        );
    }
}
