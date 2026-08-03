package dev.eduardo.artemedica.farmacia.service.strategy;

import dev.eduardo.artemedica.farmacia.model.Lote;
import dev.eduardo.artemedica.farmacia.repository.LoteRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("fefoStrategy")
public class FefoSeleccionStrategy implements LoteSeleccionStrategy {

    private final LoteRepository loteRepository;

    public FefoSeleccionStrategy(LoteRepository loteRepository) {
        this.loteRepository = loteRepository;
    }

    @Override
    public List<Lote> obtenerLotesDisponiblesParaConsumo(Long productoId) {
        return loteRepository.findLotesDisponiblesFefoForUpdate(productoId);
    }
}
