package dev.eduardo.artemedica.farmacia.dto;

import dev.eduardo.artemedica.farmacia.model.enums.OrigenMovimiento;
import dev.eduardo.artemedica.farmacia.model.enums.TipoMovimiento;

import java.time.LocalDateTime;

/** Una linea del kardex: que entro o salio, de que lote, quien lo hizo y con que saldo quedo. */
public record MovimientoInventarioResponseDTO(
        Long id,
        Long loteId,
        String numeroLote,
        Long productoId,
        String productoNombre,
        TipoMovimiento tipoMovimiento,
        Integer cantidad,
        Integer saldoResultante,
        String motivo,
        String usuario,
        LocalDateTime fechaMovimiento,
        OrigenMovimiento origenTipo,
        Long origenId
) {}
