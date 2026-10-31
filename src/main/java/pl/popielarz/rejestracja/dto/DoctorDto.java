package pl.popielarz.rejestracja.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public final class DoctorDto {

    private DoctorDto() {
    }

    @Schema(name = "DoctorRequest")
    public record Request(
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @NotBlank
            @Pattern(regexp = "\\d{7}", message = "numer prawa wykonywania zawodu musi mieć 7 cyfr")
            @Schema(example = "1234567") String licenseNumber,
            @NotBlank @Email @Size(max = 120) String email,
            @Pattern(regexp = "^$|^\\+?[0-9 ]{9,15}$", message = "niepoprawny numer telefonu")
            @Schema(example = "+48 61 123 45 67") String phone,
            @Size(max = 10) String room,
            @NotEmpty(message = "lekarz musi mieć co najmniej jedną specjalizację")
            Set<Long> specializationIds) {
    }

    @Schema(name = "DoctorResponse")
    public record Response(
            Long id,
            String firstName,
            String lastName,
            String fullName,
            String licenseNumber,
            String email,
            String phone,
            String room,
            List<SpecializationDto.Response> specializations) {
    }
}
