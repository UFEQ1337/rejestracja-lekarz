package pl.popielarz.rejestracja;

import org.springframework.test.util.ReflectionTestUtils;
import pl.popielarz.rejestracja.model.Appointment;
import pl.popielarz.rejestracja.model.Doctor;
import pl.popielarz.rejestracja.model.Patient;
import pl.popielarz.rejestracja.model.Role;
import pl.popielarz.rejestracja.model.Specialization;
import pl.popielarz.rejestracja.model.TimeSlot;
import pl.popielarz.rejestracja.security.CurrentUser;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Wspólne obiekty testowe. Stały zegar sprawia, że testy nie zależą od daty uruchomienia.
 */
public final class TestData {

    public static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    public static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 10, 0);
    public static final Clock CLOCK = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);

    private TestData() {
    }

    public static CurrentUser patientUser(long patientId) {
        return new CurrentUser(10L + patientId, "pacjent" + patientId, Role.PATIENT, patientId, null);
    }

    public static CurrentUser receptionist() {
        return new CurrentUser(3L, "rejestracja", Role.RECEPTIONIST, null, null);
    }

    public static CurrentUser doctorUser(long doctorId) {
        return new CurrentUser(20L + doctorId, "lekarz" + doctorId, Role.DOCTOR, null, doctorId);
    }

    public static Specialization specialization(long id, String name) {
        return withId(new Specialization(name), id);
    }

    public static Doctor doctor(long id) {
        Doctor doctor = new Doctor("Anna", "Kowalska", "1234561", "anna.kowalska@przychodnia-popielarz.pl");
        doctor.setRoom("101");
        return withId(doctor, id);
    }

    public static Patient mateusz() {
        return withId(new Patient("Mateusz", "Popielarz", "95031512345", "mateusz.popielarz@example.com"), 1L);
    }

    public static TimeSlot slot(long id, Doctor doctor, LocalDateTime start) {
        return withId(new TimeSlot(doctor, start, start.plusMinutes(20)), id);
    }

    public static Appointment appointment(long id, TimeSlot slot, Patient patient) {
        slot.markBooked();
        return withId(new Appointment(slot, patient, "Kontrola", NOW.minusDays(1)), id);
    }

    public static <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
