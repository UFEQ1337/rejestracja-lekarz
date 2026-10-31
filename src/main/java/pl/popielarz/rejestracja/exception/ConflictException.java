package pl.popielarz.rejestracja.exception;

/**
 * Operacja sprzeczna z aktualnym stanem danych, np. zajęty termin (HTTP 409).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
