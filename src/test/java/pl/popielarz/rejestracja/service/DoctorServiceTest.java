package pl.popielarz.rejestracja.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.popielarz.rejestracja.TestData;
import pl.popielarz.rejestracja.dto.DoctorDto;
import pl.popielarz.rejestracja.exception.ConflictException;
import pl.popielarz.rejestracja.exception.InvalidRequestException;
import pl.popielarz.rejestracja.exception.ResourceNotFoundException;
import pl.popielarz.rejestracja.mapper.DoctorMapper;
import pl.popielarz.rejestracja.mapper.SpecializationMapper;
import pl.popielarz.rejestracja.model.Doctor;
import pl.popielarz.rejestracja.repository.DoctorRepository;
import pl.popielarz.rejestracja.repository.SpecializationRepository;
import pl.popielarz.rejestracja.repository.TimeSlotRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private SpecializationRepository specializationRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;

    private DoctorService service;

    @BeforeEach
    void setUp() {
        service = new DoctorService(doctorRepository, specializationRepository, timeSlotRepository,
                new DoctorMapper(new SpecializationMapper()), TestData.CLOCK);
    }

    private static DoctorDto.Request request(Set<Long> specializationIds) {
        return new DoctorDto.Request("Adam", "Mazur", "7777777", "adam.mazur@przychodnia-popielarz.pl",
                null, "106", specializationIds);
    }

    @Test
    @DisplayName("Nowy lekarz dostaje wskazane specjalizacje (relacja N:M)")
    void shouldCreateDoctorWithSpecializations() {
        when(specializationRepository.findAllById(Set.of(3L, 1L)))
                .thenReturn(List.of(TestData.specialization(1L, "Choroby wewnętrzne"),
                        TestData.specialization(3L, "Kardiologia")));
        when(doctorRepository.save(any(Doctor.class)))
                .thenAnswer(invocation -> TestData.withId(invocation.getArgument(0), 7L));

        DoctorDto.Response result = service.create(request(Set.of(3L, 1L)));

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.fullName()).isEqualTo("Adam Mazur");
        assertThat(result.specializations()).extracting("name")
                .containsExactlyInAnyOrder("Kardiologia", "Choroby wewnętrzne");
    }

    @Test
    @DisplayName("Numer PWZ musi być unikalny (409)")
    void shouldRejectDuplicateLicenseNumber() {
        when(doctorRepository.existsByLicenseNumber("7777777")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(Set.of(1L))))
                .isInstanceOf(ConflictException.class);
        verify(doctorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nieznana specjalizacja daje błąd 400")
    void shouldRejectUnknownSpecialization() {
        when(specializationRepository.findAllById(Set.of(1L, 99L)))
                .thenReturn(List.of(TestData.specialization(1L, "Choroby wewnętrzne")));

        assertThatThrownBy(() -> service.create(request(Set.of(1L, 99L))))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Lekarza z przyszłymi wizytami nie można wycofać")
    void shouldNotDeactivateDoctorWithFutureVisits() {
        Doctor doctor = TestData.doctor(1L);
        when(doctorRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(doctor));
        when(timeSlotRepository.existsByDoctorIdAndBookedTrueAndStartTimeAfter(eq(1L), any())).thenReturn(true);

        assertThatThrownBy(() -> service.deactivate(1L)).isInstanceOf(ConflictException.class);
        assertThat(doctor.isActive()).isTrue();
    }

    @Test
    @DisplayName("Brak lekarza o podanym id daje 404")
    void shouldThrowNotFoundForMissingDoctor() {
        when(doctorRepository.findByIdAndActiveTrue(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Lekarz o id 42 nie istnieje");
    }
}
