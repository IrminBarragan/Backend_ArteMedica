package dev.eduardo.artemedica.farmacia.service.strategy;

import dev.eduardo.artemedica.farmacia.repository.ConfiguracionSistemaRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LoteSeleccionStrategyResolver {

    private static final String CLAVE_FEFO = "INVENTARIO_FEFO_HABILITADO";

    private final Map<String, LoteSeleccionStrategy> strategies;
    private final ConfiguracionSistemaRepository configuracionSistemaRepository;

    public LoteSeleccionStrategyResolver(Map<String, LoteSeleccionStrategy> strategies,
                                          ConfiguracionSistemaRepository configuracionSistemaRepository) {
        this.strategies = strategies;
        this.configuracionSistemaRepository = configuracionSistemaRepository;
    }

    public LoteSeleccionStrategy resolver() {
        boolean fefoHabilitado = configuracionSistemaRepository.findByClave(CLAVE_FEFO)
                .map(c -> Boolean.parseBoolean(c.getValor()))
                .orElse(true);
        return fefoHabilitado ? strategies.get("fefoStrategy") : strategies.get("fifoStrategy");
    }
}
