package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.ConfiguracionResponseDTO;

public interface ConfiguracionSistemaService {
    ConfiguracionResponseDTO obtenerPorClave(String clave);
    ConfiguracionResponseDTO actualizarValor(String clave, String valor, String updatedBy);
}
