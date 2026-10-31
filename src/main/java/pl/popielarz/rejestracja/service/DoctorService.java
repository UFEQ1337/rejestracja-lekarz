package pl.popielarz.rejestracja.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.popielarz.rejestracja.dto.DoctorDto;
import pl.popielarz.rejestracja.exception.ConflictException;
import pl.popielarz.rejestracja.exception.InvalidRequestException;
import pl.popielarz.rejestracja.exception.ResourceNotFoundException;
import pl.popielarz.rejestracja.mapper.DoctorMapper;
import pl.popielarz.rejestracja.model.Doctor;
import pl.popielarz.rejestracja.model.Specialization;
import pl.popielarz.rejestracja.repository.DoctorRepository;
import pl.popielarz.rejestracja.repository.SpecializationRepository;
import pl.popielarz.rejestracja.repository.TimeSlotRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);

    private final DoctorRepository doctorRepository;
    private final SpecializationRepository specializationRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final DoctorMapper doctorMapper;
    private final Clock clock;

    public DoctorService(DoctorRepository doctorRepository,
                         SpecializationRepository specializationRepository,
                         TimeSlotRepository timeSlotRepository,
                         DoctorMapper doctorMapper,
                         Clock clock) {
        this.doctorRepository = doctorRepository;
        this.specializationRepository = specializationRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.doctorMapper = doctorMapper;
        this.clock = clock;
    }

    public List<DoctorDto.Response> findAll(Long specializationId) {
        return doctorRepository.findActive(specializationId).stream()
                .map(doctorMapper::toResponse)
                .toList();
    }

    public DoctorDto.Response findById(Long id) {
        return doctorMapper.toResponse(getActiveDoctor(id));
    }

    @Transactional
    public DoctorDto.Response create(DoctorDto.Request request) {
        if (doctorRepository.existsByLicenseNumber(request.licenseNumber())) {
            throw new ConflictException("Lekarz z numerem PWZ " + request.licenseNumber() + " już istnieje");
        }
        if (doctorRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Adres e-mail " + request.email() + " jest już zajęty");
        }
        Doctor doctor = doctorMapper.toEntity(request);
        doctor.replaceSpecializations(loadSpecializations(request.specializationIds()));
        Doctor saved = doctorRepository.save(doctor);
        log.info("Dodano lekarza {} (id={})", saved.getFullName(), saved.getId());
        return doctorMapper.toResponse(saved);
    }

    @Transactional
    public DoctorDto.Response update(Long id, DoctorDto.Request request) {
        Doctor doctor = getActiveDoctor(id);
        if (!Objects.equals(doctor.getLicenseNumber(), request.licenseNumber())
                && doctorRepository.existsByLicenseNumber(request.licenseNumber())) {
            throw new ConflictException("Lekarz z numerem PWZ " + request.licenseNumber() + " już istnieje");
        }
        if (!doctor.getEmail().equalsIgnoreCase(request.email())
                && doctorRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Adres e-mail " + request.email() + " jest już zajęty");
        }
        doctorMapper.updateEntity(doctor, request);
        doctor.replaceSpecializations(loadSpecializations(request.specializationIds()));
        return doctorMapper.toResponse(doctor);
    }

    /**
     * Lekarz nie jest usuwany fizycznie, bo w bazie zostaje historia jego wizyt.
     * Nie można go wycofać, dopóki ma zarezerwowane przyszłe terminy.
     */
    @Transactional
    public void deactivate(Long id) {
        Doctor doctor = getActiveDoctor(id);
        if (timeSlotRepository.existsByDoctorIdAndBookedTrueAndStartTimeAfter(id, LocalDateTime.now(clock))) {
            throw new ConflictException("Lekarz ma zarezerwowane przyszłe wizyty - najpierw należy je anulować");
        }
        doctor.deactivate();
        log.info("Wycofano lekarza {} (id={})", doctor.getFullName(), id);
    }

    public Doctor getActiveDoctor(Long id) {
        return doctorRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Lekarz", id));
    }

    private Set<Specialization> loadSpecializations(Set<Long> ids) {
        List<Specialization> found = specializationRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            Set<Long> missing = new HashSet<>(ids);
            found.forEach(s -> missing.remove(s.getId()));
            throw new InvalidRequestException("Nieznane specjalizacje: " + missing);
        }
        return new HashSet<>(found);
    }
}
