package dev.eduardo.artemedica.farmacia.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Contrato propio de paginacion.
 *
 * Se usa en lugar de serializar directamente un Page de Spring Data porque la forma JSON de
 * PageImpl no es estable entre versiones y expone estructura interna que el cliente no necesita.
 */
public record PaginaDTO<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas,
        boolean primera,
        boolean ultima
) {
    public static <E, T> PaginaDTO<T> de(Page<E> page, Function<E, T> mapper) {
        return new PaginaDTO<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }

    public static <T> PaginaDTO<T> de(Page<T> page) {
        return de(page, Function.identity());
    }
}
