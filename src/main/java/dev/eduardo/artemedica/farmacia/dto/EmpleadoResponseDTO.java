package dev.eduardo.artemedica.farmacia.dto;

import dev.eduardo.artemedica.farmacia.model.enums.TipoEmpleado;

public record EmpleadoResponseDTO(
        Long id, String nombres, String apellidoPaterno, String apellidoMaterno,
        TipoEmpleado tipo, String especialidad, String cedulaProfesional,
        String telefonoGuardia, boolean activo
) {}
