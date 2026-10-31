package pl.popielarz.rejestracja.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class PatientDto {

    private PatientDto() {
    }

    @Schema(name = "PatientRequest")
    public record Request(
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank @Pattern(regexp = "\\d{11}", message = "PESEL musi mieć 11 cyfr")
            @Schema(example = "90010112345") String pesel,
            @NotBlank @Email @Size(max = 120) String email,
            @Pattern(regexp = "^$|^\\+?[0-9 ]{9,15}$", message = "niepoprawny numer telefonu")
            String phone,
            @Past LocalDate birthDate) {
    }

    @Schema(name = "PatientResponse")
    public record Response(
            Long id,
            String firstName,
            String lastName,
            String fullName,
            String pesel,
            String email,
            String phone,
            LocalDate birthDate) {
    }
}
