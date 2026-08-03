package dev.eduardo.artemedica.farmacia.dto;

import dev.eduardo.artemedica.farmacia.model.enums.TipoEmpleado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmpleadoRequestDTO(
        @NotBlank String nombres,
        @NotBlank String apellidoPaterno,
        String apellidoMaterno,
        @NotNull TipoEmpleado tipo,
        String especialidad,
        String cedulaProfesional,
        String telefonoGuardia
) {}
