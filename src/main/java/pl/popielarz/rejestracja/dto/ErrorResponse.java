package pl.popielarz.rejestracja.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Jednolity format odpowiedzi błędu zwracany przez całe API.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> violations) {

    public record FieldViolation(String field, String message) {
    }
}
