package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.LoteResponseDTO;

import java.time.LocalDate;
import java.util.List;

public interface LoteService {
    LoteResponseDTO obtenerPorId(Long id);
    List<LoteResponseDTO> listarActivos();
    List<LoteResponseDTO> listarPorProducto(Long productoId);
    List<LoteResponseDTO> listarVencidos();
    List<LoteResponseDTO> listarPorVencer(LocalDate fechaLimite);

    /**
     * Da de baja el lote: merma las unidades que le queden y lo marca inactivo.
     *
     * Requiere motivo y usuario porque la baja genera un movimiento de kardex. Marcar el lote
     * como inactivo sin descontar su existencia dejaria inflado Producto.stockActual.
     */
    void darDeBaja(Long id, String motivo, Long usuarioId);
}
