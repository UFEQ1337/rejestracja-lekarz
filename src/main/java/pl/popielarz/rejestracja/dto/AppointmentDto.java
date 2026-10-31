package pl.popielarz.rejestracja.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.popielarz.rejestracja.model.AppointmentStatus;

import java.time.LocalDateTime;

public final class AppointmentDto {

    private AppointmentDto() {
    }

    @Schema(name = "AppointmentCreateRequest")
    public record CreateRequest(
            @NotNull Long slotId,
            @Schema(description = "Wymagane tylko dla rejestratorki; pacjent rezerwuje zawsze dla siebie")
            Long patientId,
            @Size(max = 500) String reason) {
    }

    @Schema(name = "AppointmentStatusRequest")
    public record StatusRequest(
            @NotNull @Schema(example = "CANCELLED") AppointmentStatus status) {
    }

    @Schema(name = "AppointmentResponse")
    public record Response(
            Long id,
            AppointmentStatus status,
            String reason,
            LocalDateTime createdAt,
            LocalDateTime cancelledAt,
            Long slotId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long doctorId,
            String doctorName,
            String room,
            Long patientId,
            String patientName) {
    }
}
