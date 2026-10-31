package pl.popielarz.rejestracja.exception;

/**
 * Brak zasobu o podanym identyfikatorze (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, Long id) {
        return new ResourceNotFoundException(resource + " o id " + id + " nie istnieje");
    }
}
