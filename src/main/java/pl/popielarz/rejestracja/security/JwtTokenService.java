package pl.popielarz.rejestracja.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import pl.popielarz.rejestracja.model.UserAccount;

import java.time.Clock;
import java.time.Instant;

@Service
public class JwtTokenService {

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtTokenService(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedToken issue(UserAccount user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.validity());
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(CurrentUser.CLAIM_USER_ID, user.getId())
                .claim(CurrentUser.CLAIM_ROLE, user.getRole().name());
        if (user.getPatient() != null) {
            claims.claim(CurrentUser.CLAIM_PATIENT_ID, user.getPatient().getId());
        }
        if (user.getDoctor() != null) {
            claims.claim(CurrentUser.CLAIM_DOCTOR_ID, user.getDoctor().getId());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
