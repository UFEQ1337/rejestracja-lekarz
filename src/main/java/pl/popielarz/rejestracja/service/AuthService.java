package pl.popielarz.rejestracja.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.popielarz.rejestracja.dto.AuthDto;
import pl.popielarz.rejestracja.dto.PatientDto;
import pl.popielarz.rejestracja.exception.ConflictException;
import pl.popielarz.rejestracja.exception.ResourceNotFoundException;
import pl.popielarz.rejestracja.mapper.PatientMapper;
import pl.popielarz.rejestracja.model.Patient;
import pl.popielarz.rejestracja.model.Role;
import pl.popielarz.rejestracja.model.UserAccount;
import pl.popielarz.rejestracja.repository.UserAccountRepository;
import pl.popielarz.rejestracja.security.CurrentUser;
import pl.popielarz.rejestracja.security.JwtTokenService;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountRepository userRepository;
    private final PatientService patientService;
    private final PatientMapper patientMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;

    public AuthService(UserAccountRepository userRepository, PatientService patientService,
                       PatientMapper patientMapper, PasswordEncoder passwordEncoder,
                       JwtTokenService tokenService) {
        this.userRepository = userRepository;
        this.patientService = patientService;
        this.patientMapper = patientMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        UserAccount user = userRepository.findByUsername(request.username())
                .filter(UserAccount::isEnabled)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Nieprawidłowy login lub hasło"));
        JwtTokenService.IssuedToken token = tokenService.issue(user);
        log.info("Zalogowano użytkownika {} ({})", user.getUsername(), user.getRole());
        return new AuthDto.TokenResponse(token.value(), "Bearer", token.expiresAt(), toUserInfo(user));
    }

    /**
     * Samodzielna rejestracja pacjenta: tworzy kartę pacjenta i powiązane z nią konto.
     */
    @Transactional
    public PatientDto.Response register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Login " + request.username() + " jest już zajęty");
        }
        Patient patient = patientService.createEntity(request.patient());
        UserAccount account = new UserAccount(request.username(),
                passwordEncoder.encode(request.password()), Role.PATIENT);
        account.setPatient(patient);
        userRepository.save(account);
        return patientMapper.toResponse(patient);
    }

    public AuthDto.UserInfo me(CurrentUser currentUser) {
        return userRepository.findByUsername(currentUser.username())
                .map(this::toUserInfo)
                .orElseThrow(() -> new ResourceNotFoundException("Konto " + currentUser.username() + " nie istnieje"));
    }

    private AuthDto.UserInfo toUserInfo(UserAccount user) {
        String displayName = user.getUsername();
        if (user.getPatient() != null) {
            displayName = user.getPatient().getFullName();
        } else if (user.getDoctor() != null) {
            displayName = "lek. " + user.getDoctor().getFullName();
        } else if (user.getRole() == Role.RECEPTIONIST) {
            displayName = "Rejestracja";
        }
        return new AuthDto.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                displayName,
                user.getPatient() != null ? user.getPatient().getId() : null,
                user.getDoctor() != null ? user.getDoctor().getId() : null);
    }
}
