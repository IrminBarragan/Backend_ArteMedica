package dev.eduardo.artemedica.farmacia.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record AprobarSolicitudRequestDTO(@NotEmpty Map<Long, Integer> cantidadesAutorizadasPorProducto) {}
