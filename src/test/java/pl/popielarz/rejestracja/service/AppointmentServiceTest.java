package pl.popielarz.rejestracja.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import pl.popielarz.rejestracja.TestData;
import pl.popielarz.rejestracja.dto.AppointmentDto;
import pl.popielarz.rejestracja.exception.ConflictException;
import pl.popielarz.rejestracja.exception.InvalidRequestException;
import pl.popielarz.rejestracja.exception.ResourceNotFoundException;
import pl.popielarz.rejestracja.mapper.AppointmentMapper;
import pl.popielarz.rejestracja.model.Appointment;
import pl.popielarz.rejestracja.model.AppointmentStatus;
import pl.popielarz.rejestracja.model.Doctor;
import pl.popielarz.rejestracja.model.Patient;
import pl.popielarz.rejestracja.model.TimeSlot;
import pl.popielarz.rejestracja.repository.AppointmentRepository;
import pl.popielarz.rejestracja.repository.TimeSlotRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private PatientService patientService;

    private AppointmentService service;
    private Doctor doctor;
    private Patient patient;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepository, timeSlotRepository, patientService,
                new AppointmentMapper(), TestData.CLOCK);
        doctor = TestData.doctor(1L);
        patient = TestData.mateusz();
    }

    @Test
    @DisplayName("Pacjent rezerwuje wolny termin - termin zostaje oznaczony jako zajęty")
    void shouldBookFreeSlotForLoggedPatient() {
        TimeSlot slot = TestData.slot(5L, doctor, TestData.NOW.plusDays(1));
        when(timeSlotRepository.findById(5L)).thenReturn(Optional.of(slot));
        when(patientService.getPatient(1L)).thenReturn(patient);
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> TestData.withId(invocation.getArgument(0), 100L));

        AppointmentDto.Response result = service.book(
                new AppointmentDto.CreateRequest(5L, null, "  Ból głowy  "), TestData.patientUser(1L));

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.status()).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(result.reason()).isEqualTo("Ból głowy");
        assertThat(result.patientName()).isEqualTo("Mateusz Popielarz");
        assertThat(slot.isBooked()).isTrue();
    }

    @Test
    @DisplayName("Próba rezerwacji zajętego terminu kończy się konfliktem (409)")
    void shouldRejectBookingWhenSlotAlreadyBooked() {
        TimeSlot slot = TestData.slot(5L, doctor, TestData.NOW.plusDays(1));
        slot.markBooked();
        when(timeSlotRepository.findById(5L)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.book(new AppointmentDto.CreateRequest(5L, null, null),
                TestData.patientUser(1L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("zajęty");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nie można zarezerwować terminu z przeszłości")
    void shouldRejectBookingOfPastSlot() {
        TimeSlot slot = TestData.slot(5L, doctor, TestData.NOW.minusHours(1));
        when(timeSlotRepository.findById(5L)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.book(new AppointmentDto.CreateRequest(5L, null, null),
                TestData.patientUser(1L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("minął");
    }

    @Test
    @DisplayName("Pacjent nie może mieć dwóch wizyt w tym samym czasie")
    void shouldRejectBookingWhenPatientHasOverlappingVisit() {
        TimeSlot slot = TestData.slot(5L, doctor, TestData.NOW.plusDays(1));
        when(timeSlotRepository.findById(5L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.patientHasAppointmentBetween(1L, slot.getStartTime(), slot.getEndTime()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.book(new AppointmentDto.CreateRequest(5L, null, null),
                TestData.patientUser(1L)))
                .isInstanceOf(ConflictException.class);
        assertThat(slot.isBooked()).isFalse();
    }

    @Test
    @DisplayName("Rezerwacja nieistniejącego terminu zwraca 404")
    void shouldThrowNotFoundWhenSlotDoesNotExist() {
        when(timeSlotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.book(new AppointmentDto.CreateRequest(99L, null, null),
                TestData.patientUser(1L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Termin o id 99 nie istnieje");
    }

    @Test
    @DisplayName("Rejestratorka musi wskazać pacjenta, a pacjent nie może rezerwować za innych")
    void shouldValidatePatientIdDependingOnRole() {
        assertThatThrownBy(() -> service.book(new AppointmentDto.CreateRequest(5L, null, null),
                TestData.receptionist()))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.book(new AppointmentDto.CreateRequest(5L, 2L, null),
                TestData.patientUser(1L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Anulowanie wizyty zwalnia termin")
    void shouldCancelAppointmentAndReleaseSlot() {
        TimeSlot slot = TestData.slot(5L, doctor, TestData.NOW.plusDays(1));
        Appointment appointment = TestData.appointment(100L, slot, patient);
        when(appointmentRepository.findWithDetailsById(100L)).thenReturn(Optional.of(appointment));

        AppointmentDto.Response result = service.changeStatus(100L, AppointmentStatus.CANCELLED,
                TestData.patientUser(1L));

        assertThat(result.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(result.cancelledAt()).isEqualTo(TestData.NOW);
        assertThat(slot.isBooked()).isFalse();
    }

    @Test
    @DisplayName("Nie można drugi raz anulować tej samej wizyty")
    void shouldRejectCancellingAlreadyCancelledAppointment() {
        Appointment appointment = TestData.appointment(100L,
                TestData.slot(5L, doctor, TestData.NOW.plusDays(1)), patient);
        appointment.cancel(TestData.NOW.minusHours(2));
        when(appointmentRepository.findWithDetailsById(100L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.changeStatus(100L, AppointmentStatus.CANCELLED, TestData.patientUser(1L)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Pacjent nie ma dostępu do wizyty innego pacjenta")
    void shouldNotAllowPatientToCancelOtherPatientsAppointment() {
        Appointment appointment = TestData.appointment(100L,
                TestData.slot(5L, doctor, TestData.NOW.plusDays(1)), patient);
        when(appointmentRepository.findWithDetailsById(100L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.changeStatus(100L, AppointmentStatus.CANCELLED, TestData.patientUser(2L)))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
    }

    @Test
    @DisplayName("Lekarz oznacza odbytą wizytę dopiero po jej rozpoczęciu")
    void shouldCompleteOnlyStartedAppointment() {
        Appointment future = TestData.appointment(100L,
                TestData.slot(5L, doctor, TestData.NOW.plusDays(1)), patient);
        Appointment past = TestData.appointment(101L,
                TestData.slot(6L, doctor, TestData.NOW.minusMinutes(30)), patient);
        when(appointmentRepository.findWithDetailsById(100L)).thenReturn(Optional.of(future));
        when(appointmentRepository.findWithDetailsById(101L)).thenReturn(Optional.of(past));

        assertThatThrownBy(() -> service.changeStatus(100L, AppointmentStatus.COMPLETED, TestData.doctorUser(1L)))
                .isInstanceOf(ConflictException.class);
        assertThat(service.changeStatus(101L, AppointmentStatus.COMPLETED, TestData.doctorUser(1L)).status())
                .isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("Wyszukiwanie wizyt przez pacjenta jest zawężane do jego własnych wizyt")
    void shouldLimitSearchToOwnAppointmentsForPatient() {
        when(appointmentRepository.search(eq(1L), isNull(), isNull(), any(), any())).thenReturn(List.of());

        service.search(null, null, null, null, null, TestData.patientUser(1L));

        ArgumentCaptor<Long> patientId = ArgumentCaptor.forClass(Long.class);
        verify(appointmentRepository).search(patientId.capture(), isNull(), isNull(), any(), any());
        assertThat(patientId.getValue()).isEqualTo(1L);
    }
}
