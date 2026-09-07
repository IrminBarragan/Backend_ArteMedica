package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.AjusteRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.MermaRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.MovimientoInventarioResponseDTO;
import dev.eduardo.artemedica.farmacia.model.enums.OrigenMovimiento;

import dev.eduardo.artemedica.farmacia.dto.PaginaDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Consulta del kardex y registro de los movimientos que no provienen de una compra
 * ni de una dispensacion: mermas y ajustes manuales de inventario.
 */
public interface MovimientoInventarioService {

    PaginaDTO<MovimientoInventarioResponseDTO> listarPorProducto(Long productoId, Pageable pageable);

    PaginaDTO<MovimientoInventarioResponseDTO> listarPorLote(Long loteId, Pageable pageable);

    List<MovimientoInventarioResponseDTO> listarRecientes();

    /** Movimientos generados por un documento concreto, ej. todos los de la compra 12. */
    List<MovimientoInventarioResponseDTO> listarPorOrigen(OrigenMovimiento origenTipo, Long origenId);

    /** Da de baja unidades de un lote por perdida. Descuenta existencia y stock del producto. */
    MovimientoInventarioResponseDTO registrarMerma(MermaRequestDTO dto, Long usuarioId);

    /** Corrige las existencias de un lote en mas o en menos tras un conteo fisico. */
    MovimientoInventarioResponseDTO registrarAjuste(AjusteRequestDTO dto, Long usuarioId);

    /**
     * Da de baja el lote completo: merma la existencia que quede y lo marca inactivo.
     * Es la operacion que respalda el borrado logico de un lote caducado.
     */
    List<MovimientoInventarioResponseDTO> darDeBajaLote(Long loteId, String motivo, Long usuarioId);
}
