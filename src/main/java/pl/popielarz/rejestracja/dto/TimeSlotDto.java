package pl.popielarz.rejestracja.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class TimeSlotDto {

    private TimeSlotDto() {
    }

    @Schema(name = "TimeSlotRequest")
    public record Request(
            @NotNull @Future @Schema(example = "2026-11-20T09:00:00") LocalDateTime startTime,
            @NotNull @Schema(example = "2026-11-20T09:20:00") LocalDateTime endTime) {
    }

    @Schema(name = "TimeSlotResponse")
    public record Response(
            Long id,
            Long doctorId,
            String doctorName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean available) {
    }
}
