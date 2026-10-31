package pl.popielarz.rejestracja.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import pl.popielarz.rejestracja.TestData;
import pl.popielarz.rejestracja.dto.TimeSlotDto;
import pl.popielarz.rejestracja.exception.ConflictException;
import pl.popielarz.rejestracja.exception.InvalidRequestException;
import pl.popielarz.rejestracja.mapper.TimeSlotMapper;
import pl.popielarz.rejestracja.model.Doctor;
import pl.popielarz.rejestracja.model.TimeSlot;
import pl.popielarz.rejestracja.repository.TimeSlotRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 20, 9, 0);

    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private DoctorService doctorService;

    private TimeSlotService service;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        service = new TimeSlotService(timeSlotRepository, doctorService, new TimeSlotMapper(), TestData.CLOCK);
        doctor = TestData.doctor(1L);
    }

    @Test
    @DisplayName("Dodanie poprawnego terminu do grafiku lekarza")
    void shouldCreateSlot() {
        when(doctorService.getActiveDoctor(1L)).thenReturn(doctor);
        when(timeSlotRepository.save(any(TimeSlot.class)))
                .thenAnswer(invocation -> TestData.withId(invocation.getArgument(0), 50L));

        TimeSlotDto.Response result = service.create(1L,
                new TimeSlotDto.Request(START, START.plusMinutes(20)), TestData.receptionist());

        assertThat(result.id()).isEqualTo(50L);
        assertThat(result.available()).isTrue();
        assertThat(result.doctorName()).isEqualTo("Anna Kowalska");
    }

    @Test
    @DisplayName("Koniec terminu przed początkiem jest odrzucany (400)")
    void shouldRejectSlotEndingBeforeStart() {
        when(doctorService.getActiveDoctor(1L)).thenReturn(doctor);

        assertThatThrownBy(() -> service.create(1L,
                new TimeSlotDto.Request(START, START.minusMinutes(20)), TestData.receptionist()))
                .isInstanceOf(InvalidRequestException.class);
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Termin nachodzący na inny termin lekarza powoduje konflikt (409)")
    void shouldRejectOverlappingSlot() {
        when(doctorService.getActiveDoctor(1L)).thenReturn(doctor);
        when(timeSlotRepository.existsOverlapping(1L, START, START.plusMinutes(20))).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L,
                new TimeSlotDto.Request(START, START.plusMinutes(20)), TestData.receptionist()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Lekarz nie może dodawać terminów do grafiku innego lekarza")
    void shouldNotAllowDoctorToEditOtherSchedule() {
        assertThatThrownBy(() -> service.create(1L,
                new TimeSlotDto.Request(START, START.plusMinutes(20)), TestData.doctorUser(2L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Zarezerwowanego terminu nie można usunąć")
    void shouldNotDeleteBookedSlot() {
        TimeSlot slot = TestData.slot(50L, doctor, START);
        slot.markBooked();
        when(timeSlotRepository.findById(50L)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.delete(50L, TestData.receptionist()))
                .isInstanceOf(ConflictException.class);
        verify(timeSlotRepository, never()).delete(any());
    }
}
