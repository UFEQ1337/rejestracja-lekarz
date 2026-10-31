package pl.popielarz.rejestracja.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.popielarz.rejestracja.dto.TimeSlotDto;
import pl.popielarz.rejestracja.exception.ConflictException;
import pl.popielarz.rejestracja.exception.InvalidRequestException;
import pl.popielarz.rejestracja.exception.ResourceNotFoundException;
import pl.popielarz.rejestracja.mapper.TimeSlotMapper;
import pl.popielarz.rejestracja.model.Doctor;
import pl.popielarz.rejestracja.model.TimeSlot;
import pl.popielarz.rejestracja.repository.TimeSlotRepository;
import pl.popielarz.rejestracja.security.CurrentUser;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class TimeSlotService {

    static final Duration MIN_SLOT_LENGTH = Duration.ofMinutes(10);
    static final Duration MAX_SLOT_LENGTH = Duration.ofHours(2);
    static final int DEFAULT_RANGE_DAYS = 14;

    private final TimeSlotRepository timeSlotRepository;
    private final DoctorService doctorService;
    private final TimeSlotMapper timeSlotMapper;
    private final Clock clock;

    public TimeSlotService(TimeSlotRepository timeSlotRepository, DoctorService doctorService,
                           TimeSlotMapper timeSlotMapper, Clock clock) {
        this.timeSlotRepository = timeSlotRepository;
        this.doctorService = doctorService;
        this.timeSlotMapper = timeSlotMapper;
        this.clock = clock;
    }

    public List<TimeSlotDto.Response> findForDoctor(Long doctorId, LocalDate from, LocalDate to,
                                                    boolean onlyAvailable) {
        doctorService.getActiveDoctor(doctorId);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime start = from != null ? from.atStartOfDay() : now;
        LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay() : start.plusDays(DEFAULT_RANGE_DAYS);
        if (!end.isAfter(start)) {
            throw new InvalidRequestException("Parametr 'to' nie może być wcześniejszy niż 'from'");
        }
        if (onlyAvailable && start.isBefore(now)) {
            start = now;
        }
        return timeSlotRepository.findForDoctor(doctorId, start, end, onlyAvailable).stream()
                .map(timeSlotMapper::toResponse)
                .toList();
    }

    public TimeSlotDto.Response findById(Long id) {
        return timeSlotMapper.toResponse(getSlot(id));
    }

    @Transactional
    public TimeSlotDto.Response create(Long doctorId, TimeSlotDto.Request request, CurrentUser user) {
        if (user.isDoctor() && !Objects.equals(user.doctorId(), doctorId)) {
            throw new AccessDeniedException("Lekarz może dodawać terminy tylko we własnym grafiku");
        }
        Doctor doctor = doctorService.getActiveDoctor(doctorId);
        validateSlotTimes(request.startTime(), request.endTime());
        if (timeSlotRepository.existsOverlapping(doctorId, request.startTime(), request.endTime())) {
            throw new ConflictException("Termin nachodzi na inny termin tego lekarza");
        }
        TimeSlot slot = timeSlotRepository.save(new TimeSlot(doctor, request.startTime(), request.endTime()));
        return timeSlotMapper.toResponse(slot);
    }

    @Transactional
    public void delete(Long id, CurrentUser user) {
        TimeSlot slot = getSlot(id);
        if (user.isDoctor() && !Objects.equals(user.doctorId(), slot.getDoctor().getId())) {
            throw new AccessDeniedException("Lekarz może usuwać terminy tylko z własnego grafiku");
        }
        if (slot.isBooked()) {
            throw new ConflictException("Termin jest zarezerwowany - najpierw należy anulować wizytę");
        }
        timeSlotRepository.delete(slot);
    }

    private TimeSlot getSlot(Long id) {
        return timeSlotRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Termin", id));
    }

    private void validateSlotTimes(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new InvalidRequestException("Koniec terminu musi być późniejszy niż jego początek");
        }
        Duration length = Duration.between(start, end);
        if (length.compareTo(MIN_SLOT_LENGTH) < 0 || length.compareTo(MAX_SLOT_LENGTH) > 0) {
            throw new InvalidRequestException("Termin musi trwać od 10 minut do 2 godzin");
        }
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new InvalidRequestException("Termin musi zaczynać się i kończyć tego samego dnia");
        }
    }
}
