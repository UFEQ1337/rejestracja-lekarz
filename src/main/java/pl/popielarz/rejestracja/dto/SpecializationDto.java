package pl.popielarz.rejestracja.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class SpecializationDto {

    private SpecializationDto() {
    }

    @Schema(name = "SpecializationRequest")
    public record Request(
            @NotBlank @Size(max = 100) String name) {
    }

    @Schema(name = "SpecializationResponse")
    public record Response(Long id, String name) {
    }
}
