package pl.popielarz.rejestracja.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.popielarz.rejestracja.config.OpenApiConfig;
import pl.popielarz.rejestracja.dto.SpecializationDto;
import pl.popielarz.rejestracja.service.SpecializationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specializations")
@Tag(name = "Specjalizacje", description = "Słownik specjalizacji lekarskich")
public class SpecializationController {

    private final SpecializationService specializationService;

    public SpecializationController(SpecializationService specializationService) {
        this.specializationService = specializationService;
    }

    @GetMapping
    @Operation(summary = "Lista specjalizacji (zasób publiczny)")
    public List<SpecializationDto.Response> findAll() {
        return specializationService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RECEPTIONIST')")
    @Operation(summary = "Dodanie specjalizacji", security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    @ApiResponse(responseCode = "201", description = "Utworzono specjalizację")
    @ApiResponse(responseCode = "409", description = "Specjalizacja o tej nazwie już istnieje")
    public SpecializationDto.Response create(@Valid @RequestBody SpecializationDto.Request request) {
        return specializationService.create(request);
    }
}
