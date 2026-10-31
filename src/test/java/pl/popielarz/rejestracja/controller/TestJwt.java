package pl.popielarz.rejestracja.controller;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import pl.popielarz.rejestracja.model.Role;
import pl.popielarz.rejestracja.security.CurrentUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Tokeny JWT do testów kontrolerów - odpowiadają tym, które wystawia AuthService.
 */
final class TestJwt {

    private TestJwt() {
    }

    static JwtRequestPostProcessor patient(long patientId) {
        return jwt().jwt(token -> token.subject("mpopielarz")
                        .claim(CurrentUser.CLAIM_USER_ID, 1L)
                        .claim(CurrentUser.CLAIM_ROLE, Role.PATIENT.name())
                        .claim(CurrentUser.CLAIM_PATIENT_ID, patientId))
                .authorities(new SimpleGrantedAuthority("ROLE_PATIENT"));
    }

    static JwtRequestPostProcessor receptionist() {
        return jwt().jwt(token -> token.subject("rejestracja")
                        .claim(CurrentUser.CLAIM_USER_ID, 3L)
                        .claim(CurrentUser.CLAIM_ROLE, Role.RECEPTIONIST.name()))
                .authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"));
    }
}
