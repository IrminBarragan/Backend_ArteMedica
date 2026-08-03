package dev.eduardo.artemedica.farmacia.service.strategy;

import dev.eduardo.artemedica.farmacia.model.Lote;
import dev.eduardo.artemedica.farmacia.repository.LoteRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("fifoStrategy")
public class FifoSeleccionStrategy implements LoteSeleccionStrategy {

    private final LoteRepository loteRepository;

    public FifoSeleccionStrategy(LoteRepository loteRepository) {
        this.loteRepository = loteRepository;
    }

    @Override
    public List<Lote> obtenerLotesDisponiblesParaConsumo(Long productoId) {
        return loteRepository.findLotesDisponiblesFifoForUpdate(productoId);
    }
}
