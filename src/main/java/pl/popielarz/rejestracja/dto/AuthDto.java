package pl.popielarz.rejestracja.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pl.popielarz.rejestracja.model.Role;

import java.time.Instant;

public final class AuthDto {

    private AuthDto() {
    }

    @Schema(name = "LoginRequest")
    public record LoginRequest(
            @NotBlank @Schema(example = "mpopielarz") String username,
            @NotBlank @Schema(example = "Pacjent123!") String password) {
    }

    @Schema(name = "RegisterRequest")
    public record RegisterRequest(
            @NotBlank @Size(min = 4, max = 50)
            @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "dozwolone są litery, cyfry oraz . _ -")
            String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotNull @Valid PatientDto.Request patient) {
    }

    @Schema(name = "TokenResponse")
    public record TokenResponse(
            String accessToken,
            String tokenType,
            Instant expiresAt,
            UserInfo user) {
    }

    @Schema(name = "UserInfo")
    public record UserInfo(
            Long id,
            String username,
            Role role,
            String displayName,
            Long patientId,
            Long doctorId) {
    }
}
