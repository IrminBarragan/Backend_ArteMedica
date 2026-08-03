package dev.eduardo.artemedica.farmacia.exception;

public class ConflictoConcurrenciaException extends RuntimeException {
    public ConflictoConcurrenciaException(String message) {
        super(message);
    }
}
