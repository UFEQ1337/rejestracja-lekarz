package pl.popielarz.rejestracja.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import pl.popielarz.rejestracja.dto.AppointmentDto;
import pl.popielarz.rejestracja.dto.PatientDto;
import pl.popielarz.rejestracja.security.CurrentUser;
import pl.popielarz.rejestracja.service.AppointmentService;
import pl.popielarz.rejestracja.service.PatientService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Pacjenci", description = "Kartoteka pacjentów")
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class PatientController {

    private final PatientService patientService;
    private final AppointmentService appointmentService;

    public PatientController(PatientService patientService, AppointmentService appointmentService) {
        this.patientService = patientService;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'DOCTOR')")
    @Operation(summary = "Lista pacjentów, opcjonalnie wyszukiwanie po początku nazwiska")
    public List<PatientDto.Response> findAll(@RequestParam(required = false) String lastName) {
        return patientService.findAll(lastName);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Dane pacjenta (pacjent widzi tylko siebie)")
    @ApiResponse(responseCode = "403", description = "Pacjent próbuje odczytać cudze dane")
    @ApiResponse(responseCode = "404", description = "Pacjent nie istnieje")
    public PatientDto.Response findById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return patientService.findById(id, CurrentUser.from(jwt));
    }

    @PostMapping
    @PreAuthorize("hasRole('RECEPTIONIST')")
    @Operation(summary = "Rejestracja pacjenta w okienku (bez konta)")
    @ApiResponse(responseCode = "201", description = "Utworzono kartę pacjenta")
    @ApiResponse(responseCode = "409", description = "PESEL lub e-mail już istnieje")
    public ResponseEntity<PatientDto.Response> create(@Valid @RequestBody PatientDto.Request request) {
        PatientDto.Response created = patientService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'PATIENT')")
    @Operation(summary = "Aktualizacja danych pacjenta")
    public PatientDto.Response update(@PathVariable Long id, @Valid @RequestBody PatientDto.Request request,
                                      @AuthenticationPrincipal Jwt jwt) {
        return patientService.update(id, request, CurrentUser.from(jwt));
    }

    @GetMapping("/{id}/appointments")
    @Operation(summary = "Historia i plan wizyt pacjenta")
    public List<AppointmentDto.Response> findAppointments(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser user = CurrentUser.from(jwt);
        patientService.findById(id, user);
        return appointmentService.search(id, null, null, null, null, user);
    }
}
