package pl.popielarz.rejestracja.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.popielarz.rejestracja.dto.PatientDto;
import pl.popielarz.rejestracja.exception.ConflictException;
import pl.popielarz.rejestracja.exception.ResourceNotFoundException;
import pl.popielarz.rejestracja.mapper.PatientMapper;
import pl.popielarz.rejestracja.model.Patient;
import pl.popielarz.rejestracja.repository.PatientRepository;
import pl.popielarz.rejestracja.security.CurrentUser;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    public PatientService(PatientRepository patientRepository, PatientMapper patientMapper) {
        this.patientRepository = patientRepository;
        this.patientMapper = patientMapper;
    }

    public List<PatientDto.Response> findAll(String lastName) {
        List<Patient> patients = (lastName == null || lastName.isBlank())
                ? patientRepository.findAllByOrderByLastNameAscFirstNameAsc()
                : patientRepository.findByLastNameStartingWithIgnoreCaseOrderByLastNameAscFirstNameAsc(lastName.trim());
        return patients.stream().map(patientMapper::toResponse).toList();
    }

    public PatientDto.Response findById(Long id, CurrentUser user) {
        checkAccess(id, user);
        return patientMapper.toResponse(getPatient(id));
    }

    @Transactional
    public Patient createEntity(PatientDto.Request request) {
        if (patientRepository.existsByPesel(request.pesel())) {
            throw new ConflictException("Pacjent z podanym numerem PESEL jest już zarejestrowany");
        }
        if (patientRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Adres e-mail " + request.email() + " jest już zajęty");
        }
        Patient saved = patientRepository.save(patientMapper.toEntity(request));
        log.info("Zarejestrowano pacjenta id={}", saved.getId());
        return saved;
    }

    @Transactional
    public PatientDto.Response create(PatientDto.Request request) {
        return patientMapper.toResponse(createEntity(request));
    }

    @Transactional
    public PatientDto.Response update(Long id, PatientDto.Request request, CurrentUser user) {
        checkAccess(id, user);
        Patient patient = getPatient(id);
        if (!patient.getPesel().equals(request.pesel()) && patientRepository.existsByPesel(request.pesel())) {
            throw new ConflictException("Pacjent z podanym numerem PESEL jest już zarejestrowany");
        }
        if (!patient.getEmail().equalsIgnoreCase(request.email())
                && patientRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Adres e-mail " + request.email() + " jest już zajęty");
        }
        patientMapper.updateEntity(patient, request);
        return patientMapper.toResponse(patient);
    }

    public Patient getPatient(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Pacjent", id));
    }

    /**
     * Pacjent ma dostęp wyłącznie do własnych danych.
     */
    public void checkAccess(Long patientId, CurrentUser user) {
        if (user.isPatient() && !Objects.equals(user.patientId(), patientId)) {
            throw new AccessDeniedException("Pacjent ma dostęp tylko do własnych danych");
        }
    }
}
