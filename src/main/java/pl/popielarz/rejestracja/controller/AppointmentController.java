package pl.popielarz.rejestracja.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.popielarz.rejestracja.config.OpenApiConfig;
import pl.popielarz.rejestracja.dto.AppointmentDto;
import pl.popielarz.rejestracja.model.AppointmentStatus;
import pl.popielarz.rejestracja.security.CurrentUser;
import pl.popielarz.rejestracja.service.AppointmentService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Wizyty", description = "Rezerwacja, przegląd i anulowanie wizyt")
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    @Operation(summary = "Wyszukiwanie wizyt (pacjent widzi swoje, lekarz swoje, rejestracja wszystkie)")
    public List<AppointmentDto.Response> search(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal Jwt jwt) {
        return appointmentService.search(patientId, doctorId, status, from, to, CurrentUser.from(jwt));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Szczegóły wizyty")
    @ApiResponse(responseCode = "403", description = "Wizyta należy do innego pacjenta")
    @ApiResponse(responseCode = "404", description = "Wizyta nie istnieje")
    public AppointmentDto.Response findById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return appointmentService.findById(id, CurrentUser.from(jwt));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT', 'RECEPTIONIST')")
    @Operation(summary = "Rezerwacja wizyty w wolnym terminie")
    @ApiResponse(responseCode = "201", description = "Wizyta zarezerwowana")
    @ApiResponse(responseCode = "400", description = "Błąd walidacji")
    @ApiResponse(responseCode = "404", description = "Termin lub pacjent nie istnieje")
    @ApiResponse(responseCode = "409", description = "Termin zajęty, miniony lub kolizja z inną wizytą pacjenta")
    public ResponseEntity<AppointmentDto.Response> book(@Valid @RequestBody AppointmentDto.CreateRequest request,
                                                        @AuthenticationPrincipal Jwt jwt) {
        AppointmentDto.Response created = appointmentService.book(request, CurrentUser.from(jwt));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Zmiana statusu wizyty: CANCELLED (anulowanie) lub COMPLETED (wizyta odbyta)")
    @ApiResponse(responseCode = "200", description = "Status zmieniony")
    @ApiResponse(responseCode = "409", description = "Wizyta nie jest już aktywna lub termin minął")
    public AppointmentDto.Response changeStatus(@PathVariable Long id,
                                                @Valid @RequestBody AppointmentDto.StatusRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
        return appointmentService.changeStatus(id, request.status(), CurrentUser.from(jwt));
    }
}
