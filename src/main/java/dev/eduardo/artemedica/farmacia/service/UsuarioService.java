package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.LoginResponseDTO;
import dev.eduardo.artemedica.farmacia.dto.UsuarioRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.UsuarioResponseDTO;

import java.util.List;

public interface UsuarioService {
    UsuarioResponseDTO crear(UsuarioRequestDTO dto);
    UsuarioResponseDTO actualizar(Long id, UsuarioRequestDTO dto);
    UsuarioResponseDTO obtenerPorId(Long id);
    List<UsuarioResponseDTO> listarActivos();
    void desactivar(Long id);
    LoginResponseDTO autenticar(String username, String password);
}
