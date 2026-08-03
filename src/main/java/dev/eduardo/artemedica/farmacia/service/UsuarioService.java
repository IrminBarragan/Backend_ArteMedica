package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.UsuarioRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.UsuarioResponseDTO;

import java.util.List;

public interface UsuarioService {
    UsuarioResponseDTO crear(UsuarioRequestDTO dto);
    UsuarioResponseDTO actualizar(Long id, UsuarioRequestDTO dto);
    UsuarioResponseDTO obtenerPorId(Long id);
    List<UsuarioResponseDTO> listarActivos();
    void desactivar(Long id);

    // Solo valida credenciales (lanza AutenticacionException si no coinciden); el JWT se genera en AuthController
    void autenticar(String username, String password);
}
