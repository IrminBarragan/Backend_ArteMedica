package dev.eduardo.artemedica.farmacia.service.impl;

import dev.eduardo.artemedica.farmacia.dto.ConfiguracionResponseDTO;
import dev.eduardo.artemedica.farmacia.exception.ResourceNotFoundException;
import dev.eduardo.artemedica.farmacia.model.ConfiguracionSistema;
import dev.eduardo.artemedica.farmacia.repository.ConfiguracionSistemaRepository;
import dev.eduardo.artemedica.farmacia.service.ConfiguracionSistemaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ConfiguracionSistemaServiceImpl implements ConfiguracionSistemaService {

    private final ConfiguracionSistemaRepository configuracionSistemaRepository;

    public ConfiguracionSistemaServiceImpl(ConfiguracionSistemaRepository configuracionSistemaRepository) {
        this.configuracionSistemaRepository = configuracionSistemaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionResponseDTO obtenerPorClave(String clave) {
        return toDto(obtenerEntidad(clave));
    }

    @Override
    @Transactional
    public ConfiguracionResponseDTO actualizarValor(String clave, String valor, String updatedBy) {
        ConfiguracionSistema configuracion = obtenerEntidad(clave);
        configuracion.setValor(valor);
        configuracion.setUpdatedAt(LocalDateTime.now());
        configuracion.setUpdatedBy(updatedBy);
        return toDto(configuracionSistemaRepository.save(configuracion));
    }

    private ConfiguracionSistema obtenerEntidad(String clave) {
        return configuracionSistemaRepository.findByClave(clave)
                .orElseThrow(() -> new ResourceNotFoundException("Configuracion no encontrada: " + clave));
    }

    private ConfiguracionResponseDTO toDto(ConfiguracionSistema configuracion) {
        return new ConfiguracionResponseDTO(
                configuracion.getClave(), configuracion.getValor(), configuracion.getDescripcion(),
                configuracion.getUpdatedAt(), configuracion.getUpdatedBy()
        );
    }
}
