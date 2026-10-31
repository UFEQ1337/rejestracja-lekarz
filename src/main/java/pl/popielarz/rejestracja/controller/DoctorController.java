package pl.popielarz.rejestracja.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.popielarz.rejestracja.config.OpenApiConfig;
import pl.popielarz.rejestracja.dto.DoctorDto;
import pl.popielarz.rejestracja.dto.TimeSlotDto;
import pl.popielarz.rejestracja.security.CurrentUser;
import pl.popielarz.rejestracja.service.DoctorService;
import pl.popielarz.rejestracja.service.TimeSlotService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors")
@Tag(name = "Lekarze", description = "Lekarze i ich terminy przyjęć")
public class DoctorController {

    private final DoctorService doctorService;
    private final TimeSlotService timeSlotService;

    public DoctorController(DoctorService doctorService, TimeSlotService timeSlotService) {
        this.doctorService = doctorService;
        this.timeSlotService = timeSlotService;
    }

    @GetMapping
    @Operation(summary = "Lista lekarzy, opcjonalnie filtrowana po specjalizacji")
    public List<DoctorDto.Response> findAll(
            @Parameter(description = "Identyfikator specjalizacji")
            @RequestParam(required = false) Long specializationId) {
        return doctorService.findAll(specializationId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Szczegóły lekarza")
    @ApiResponse(responseCode = "200", description = "Znaleziono lekarza")
    @ApiResponse(responseCode = "404", description = "Lekarz nie istnieje")
    public DoctorDto.Response findById(@PathVariable Long id) {
        return doctorService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('RECEPTIONIST')")
    @Operation(summary = "Dodanie lekarza", security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    @ApiResponse(responseCode = "201", description = "Utworzono lekarza, nagłówek Location wskazuje nowy zasób")
    @ApiResponse(responseCode = "400", description = "Błąd walidacji lub nieznana specjalizacja")
    @ApiResponse(responseCode = "409", description = "Numer PWZ lub e-mail już istnieje")
    public ResponseEntity<DoctorDto.Response> create(@Valid @RequestBody DoctorDto.Request request) {
        DoctorDto.Response created = doctorService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECEPTIONIST')")
    @Operation(summary = "Aktualizacja danych lekarza", security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    @ApiResponse(responseCode = "200", description = "Zaktualizowano")
    @ApiResponse(responseCode = "404", description = "Lekarz nie istnieje")
    public DoctorDto.Response update(@PathVariable Long id, @Valid @RequestBody DoctorDto.Request request) {
        return doctorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RECEPTIONIST')")
    @Operation(summary = "Wycofanie lekarza z rejestracji",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    @ApiResponse(responseCode = "204", description = "Lekarz wycofany")
    @ApiResponse(responseCode = "409", description = "Lekarz ma zarezerwowane przyszłe wizyty")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        doctorService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/slots")
    @Operation(summary = "Terminy lekarza w zadanym przedziale dat (domyślnie wolne, 14 dni od dziś)")
    public List<TimeSlotDto.Response> findSlots(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "true - tylko wolne terminy")
            @RequestParam(defaultValue = "true") boolean available) {
        return timeSlotService.findForDoctor(id, from, to, available);
    }

    @PostMapping("/{id}/slots")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'DOCTOR')")
    @Operation(summary = "Dodanie terminu przyjęć", security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    @ApiResponse(responseCode = "201", description = "Utworzono termin")
    @ApiResponse(responseCode = "400", description = "Niepoprawne godziny terminu")
    @ApiResponse(responseCode = "409", description = "Termin nachodzi na istniejący")
    public ResponseEntity<TimeSlotDto.Response> createSlot(@PathVariable Long id,
                                                           @Valid @RequestBody TimeSlotDto.Request request,
                                                           @AuthenticationPrincipal Jwt jwt) {
        TimeSlotDto.Response created = timeSlotService.create(id, request, CurrentUser.from(jwt));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/slots/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }
}
