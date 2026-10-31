package pl.popielarz.rejestracja.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.popielarz.rejestracja.config.OpenApiConfig;
import pl.popielarz.rejestracja.dto.TimeSlotDto;
import pl.popielarz.rejestracja.security.CurrentUser;
import pl.popielarz.rejestracja.service.TimeSlotService;

@RestController
@RequestMapping("/api/v1/slots")
@Tag(name = "Terminy", description = "Pojedyncze terminy przyjęć")
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    public TimeSlotController(TimeSlotService timeSlotService) {
        this.timeSlotService = timeSlotService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Szczegóły terminu")
    @ApiResponse(responseCode = "404", description = "Termin nie istnieje")
    public TimeSlotDto.Response findById(@PathVariable Long id) {
        return timeSlotService.findById(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'DOCTOR')")
    @Operation(summary = "Usunięcie wolnego terminu z grafiku")
    @ApiResponse(responseCode = "204", description = "Termin usunięty")
    @ApiResponse(responseCode = "409", description = "Termin jest zarezerwowany")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        timeSlotService.delete(id, CurrentUser.from(jwt));
        return ResponseEntity.noContent().build();
    }
}
