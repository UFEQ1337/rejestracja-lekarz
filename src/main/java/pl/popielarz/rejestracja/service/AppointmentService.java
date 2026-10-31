package pl.popielarz.rejestracja.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.popielarz.rejestracja.dto.AppointmentDto;
import pl.popielarz.rejestracja.exception.ConflictException;
import pl.popielarz.rejestracja.exception.InvalidRequestException;
import pl.popielarz.rejestracja.exception.ResourceNotFoundException;
import pl.popielarz.rejestracja.mapper.AppointmentMapper;
import pl.popielarz.rejestracja.model.Appointment;
import pl.popielarz.rejestracja.model.AppointmentStatus;
import pl.popielarz.rejestracja.model.Patient;
import pl.popielarz.rejestracja.model.TimeSlot;
import pl.popielarz.rejestracja.repository.AppointmentRepository;
import pl.popielarz.rejestracja.repository.TimeSlotRepository;
import pl.popielarz.rejestracja.security.CurrentUser;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);
    private static final LocalDateTime RANGE_START = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime RANGE_END = LocalDateTime.of(2100, 1, 1, 0, 0);

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final PatientService patientService;
    private final AppointmentMapper appointmentMapper;
    private final Clock clock;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              TimeSlotRepository timeSlotRepository,
                              PatientService patientService,
                              AppointmentMapper appointmentMapper,
                              Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.patientService = patientService;
        this.appointmentMapper = appointmentMapper;
        this.clock = clock;
    }

    /**
     * Rezerwacja wizyty. Najważniejsza reguła biznesowa: jeden termin może mieć
     * tylko jedną aktywną wizytę (w przeciwnym razie 409 Conflict).
     */
    @Transactional
    public AppointmentDto.Response book(AppointmentDto.CreateRequest request, CurrentUser user) {
        Long patientId = resolvePatientId(request.patientId(), user);
        TimeSlot slot = timeSlotRepository.findById(request.slotId())
                .orElseThrow(() -> ResourceNotFoundException.of("Termin", request.slotId()));
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);

        if (!slot.getDoctor().isActive()) {
            throw new ConflictException("Lekarz nie przyjmuje już pacjentów");
        }
        if (slot.startsBefore(now)) {
            throw new ConflictException("Nie można zarezerwować terminu, który już minął");
        }
        if (slot.isBooked()) {
            throw new ConflictException("Wybrany termin jest już zajęty");
        }
        if (appointmentRepository.patientHasAppointmentBetween(patientId, slot.getStartTime(), slot.getEndTime())) {
            throw new ConflictException("Pacjent ma już wizytę w tym samym czasie");
        }

        Patient patient = patientService.getPatient(patientId);
        slot.markBooked();
        Appointment appointment = appointmentRepository.save(
                new Appointment(slot, patient, normalize(request.reason()), now));
        log.info("Zarezerwowano wizytę id={} (pacjent id={}, termin id={})",
                appointment.getId(), patientId, slot.getId());
        return appointmentMapper.toResponse(appointment);
    }

    public AppointmentDto.Response findById(Long id, CurrentUser user) {
        Appointment appointment = getAppointment(id);
        checkAccess(appointment, user);
        return appointmentMapper.toResponse(appointment);
    }

    public List<AppointmentDto.Response> search(Long patientId, Long doctorId, AppointmentStatus status,
                                                LocalDate from, LocalDate to, CurrentUser user) {
        if (user.isPatient()) {
            if (patientId != null && !patientId.equals(user.patientId())) {
                throw new AccessDeniedException("Pacjent może przeglądać tylko własne wizyty");
            }
            patientId = user.patientId();
        } else if (user.isDoctor()) {
            doctorId = user.doctorId();
        }
        LocalDateTime start = from != null ? from.atStartOfDay() : RANGE_START;
        LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay() : RANGE_END;
        return appointmentRepository.search(patientId, doctorId, status, start, end).stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }

    /**
     * Zmiana statusu wizyty (PATCH). Pacjent i rejestratorka mogą wizytę anulować,
     * lekarz i rejestratorka - oznaczyć jako odbytą.
     */
    @Transactional
    public AppointmentDto.Response changeStatus(Long id, AppointmentStatus newStatus, CurrentUser user) {
        Appointment appointment = getAppointment(id);
        checkAccess(appointment, user);
        if (!appointment.isBooked()) {
            throw new ConflictException("Wizyta ma już status " + appointment.getStatus());
        }
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        switch (newStatus) {
            case CANCELLED -> cancel(appointment, now, user);
            case COMPLETED -> complete(appointment, now, user);
            case BOOKED -> throw new InvalidRequestException("Wizyta jest już zarezerwowana");
        }
        return appointmentMapper.toResponse(appointment);
    }

    private void cancel(Appointment appointment, LocalDateTime now, CurrentUser user) {
        if (user.isDoctor()) {
            throw new AccessDeniedException("Lekarz nie anuluje wizyt - robi to pacjent lub rejestracja");
        }
        if (appointment.getSlot().startsBefore(now)) {
            throw new ConflictException("Nie można anulować wizyty, która już się rozpoczęła");
        }
        appointment.cancel(now);
        log.info("Anulowano wizytę id={} przez {}", appointment.getId(), user.username());
    }

    private void complete(Appointment appointment, LocalDateTime now, CurrentUser user) {
        if (user.isPatient()) {
            throw new AccessDeniedException("Pacjent nie może oznaczyć wizyty jako odbytej");
        }
        if (!appointment.getSlot().startsBefore(now)) {
            throw new ConflictException("Wizyty nie można zakończyć przed jej terminem");
        }
        appointment.complete();
        log.info("Wizyta id={} oznaczona jako odbyta", appointment.getId());
    }

    private Long resolvePatientId(Long requestedPatientId, CurrentUser user) {
        if (user.isPatient()) {
            if (requestedPatientId != null && !requestedPatientId.equals(user.patientId())) {
                throw new AccessDeniedException("Pacjent może rezerwować wizyty tylko dla siebie");
            }
            return user.patientId();
        }
        if (requestedPatientId == null) {
            throw new InvalidRequestException("Pole patientId jest wymagane przy rezerwacji przez rejestrację");
        }
        return requestedPatientId;
    }

    private Appointment getAppointment(Long id) {
        return appointmentRepository.findWithDetailsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Wizyta", id));
    }

    private void checkAccess(Appointment appointment, CurrentUser user) {
        boolean allowed = switch (user.role()) {
            case RECEPTIONIST -> true;
            case PATIENT -> Objects.equals(appointment.getPatient().getId(), user.patientId());
            case DOCTOR -> Objects.equals(appointment.getSlot().getDoctor().getId(), user.doctorId());
        };
        if (!allowed) {
            throw new AccessDeniedException("Brak dostępu do tej wizyty");
        }
    }

    private static String normalize(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
