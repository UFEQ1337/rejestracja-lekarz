package pl.popielarz.rejestracja.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pl.popielarz.rejestracja.config.OpenApiConfig;
import pl.popielarz.rejestracja.dto.AuthDto;
import pl.popielarz.rejestracja.dto.PatientDto;
import pl.popielarz.rejestracja.security.CurrentUser;
import pl.popielarz.rejestracja.service.AuthService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Uwierzytelnianie", description = "Logowanie (token JWT) i rejestracja konta pacjenta")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Logowanie - zwraca token JWT do nagłówka Authorization")
    @ApiResponse(responseCode = "200", description = "Poprawne logowanie")
    @ApiResponse(responseCode = "401", description = "Nieprawidłowy login lub hasło")
    public AuthDto.TokenResponse login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @Operation(summary = "Rejestracja nowego pacjenta wraz z kontem")
    @ApiResponse(responseCode = "201", description = "Konto i karta pacjenta utworzone")
    @ApiResponse(responseCode = "400", description = "Błąd walidacji")
    @ApiResponse(responseCode = "409", description = "Login, PESEL lub e-mail już zajęty")
    public ResponseEntity<PatientDto.Response> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        PatientDto.Response patient = authService.register(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/patients/{id}").buildAndExpand(patient.id()).toUri();
        return ResponseEntity.created(location).body(patient);
    }

    @GetMapping("/me")
    @Operation(summary = "Dane zalogowanego użytkownika", security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    public AuthDto.UserInfo me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(CurrentUser.from(jwt));
    }
}
