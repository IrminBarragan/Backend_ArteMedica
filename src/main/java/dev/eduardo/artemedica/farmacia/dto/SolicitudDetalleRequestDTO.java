package dev.eduardo.artemedica.farmacia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SolicitudDetalleRequestDTO(@NotNull Long productoId, @NotNull @Positive Integer cantidadSolicitada) {}
