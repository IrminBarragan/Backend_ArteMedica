package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.LoteResponseDTO;
import dev.eduardo.artemedica.farmacia.dto.PaginaDTO;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface LoteService {
    LoteResponseDTO obtenerPorId(Long id);
    PaginaDTO<LoteResponseDTO> listarActivos(Pageable pageable);
    PaginaDTO<LoteResponseDTO> listarPorProducto(Long productoId, Pageable pageable);
    PaginaDTO<LoteResponseDTO> listarVencidos(Pageable pageable);
    PaginaDTO<LoteResponseDTO> listarPorVencer(LocalDate fechaLimite, Pageable pageable);

    /**
     * Da de baja el lote: merma las unidades que le queden y lo marca inactivo.
     *
     * Requiere motivo y usuario porque la baja genera un movimiento de kardex. Marcar el lote
     * como inactivo sin descontar su existencia dejaria inflado Producto.stockActual.
     */
    void darDeBaja(Long id, String motivo, Long usuarioId);
}
