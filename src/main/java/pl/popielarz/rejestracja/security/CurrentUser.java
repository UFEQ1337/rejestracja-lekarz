package pl.popielarz.rejestracja.security;

import org.springframework.security.oauth2.jwt.Jwt;
import pl.popielarz.rejestracja.model.Role;

/**
 * Dane zalogowanego użytkownika odczytane z tokena JWT. Serwer nie przechowuje
 * sesji, więc każde żądanie niesie komplet informacji potrzebnych do autoryzacji.
 */
public record CurrentUser(Long userId, String username, Role role, Long patientId, Long doctorId) {

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_PATIENT_ID = "patientId";
    public static final String CLAIM_DOCTOR_ID = "doctorId";

    public static CurrentUser from(Jwt jwt) {
        return new CurrentUser(
                toLong(jwt.getClaim(CLAIM_USER_ID)),
                jwt.getSubject(),
                Role.valueOf(jwt.getClaimAsString(CLAIM_ROLE)),
                toLong(jwt.getClaim(CLAIM_PATIENT_ID)),
                toLong(jwt.getClaim(CLAIM_DOCTOR_ID)));
    }

    public boolean isPatient() {
        return role == Role.PATIENT;
    }

    public boolean isDoctor() {
        return role == Role.DOCTOR;
    }

    public boolean isReceptionist() {
        return role == Role.RECEPTIONIST;
    }

    private static Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
