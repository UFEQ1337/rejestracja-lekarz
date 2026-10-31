package pl.popielarz.rejestracja.exception;

/**
 * Dane poprawne składniowo, ale niespełniające reguł biznesowych (HTTP 400).
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
