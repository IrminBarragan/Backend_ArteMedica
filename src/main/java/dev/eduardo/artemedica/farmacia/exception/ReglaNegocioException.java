package dev.eduardo.artemedica.farmacia.exception;

/**
 * La peticion es sintacticamente valida pero viola una regla del dominio,
 * ej. autorizar mas unidades de las que se solicitaron. Se traduce a 400.
 */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
