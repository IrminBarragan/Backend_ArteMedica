package dev.eduardo.artemedica.farmacia.service.strategy;

import dev.eduardo.artemedica.farmacia.model.Lote;

import java.util.List;

public interface LoteSeleccionStrategy {
    List<Lote> obtenerLotesDisponiblesParaConsumo(Long productoId);
}
