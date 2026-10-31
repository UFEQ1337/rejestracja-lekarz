package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generuje grafik demonstracyjny względem daty uruchomienia migracji: terminy od tygodnia
 * wstecz do dwóch tygodni naprzód (dni robocze, wizyty po 20 minut) oraz przykładowe wizyty.
 * Migracja w Javie, bo statyczny skrypt SQL miałby terminy szybko nieaktualne.
 */
public class V3__Demo_time_slots extends BaseJavaMigration {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final int SLOT_MINUTES = 20;
    private static final int SLOTS_PER_DAY = 12;

    /** Numer PWZ lekarza -> godzina rozpoczęcia przyjęć. */
    private static final Map<String, LocalTime> SCHEDULE = new LinkedHashMap<>();

    static {
        SCHEDULE.put("1234561", LocalTime.of(8, 0));
        SCHEDULE.put("2345672", LocalTime.of(12, 0));
        SCHEDULE.put("3456783", LocalTime.of(8, 0));
        SCHEDULE.put("4567894", LocalTime.of(12, 0));
        SCHEDULE.put("5678905", LocalTime.of(8, 0));
        SCHEDULE.put("6789016", LocalTime.of(12, 0));
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime now = LocalDateTime.now(ZONE).truncatedTo(ChronoUnit.MINUTES);

        long mateusz = findId(connection, "SELECT id FROM patient WHERE pesel = '95031512345'");
        long joanna = findId(connection, "SELECT id FROM patient WHERE pesel = '88072254321'");
        List<Long> otherPatients = List.of(
                findId(connection, "SELECT id FROM patient WHERE pesel = '76110398765'"),
                findId(connection, "SELECT id FROM patient WHERE pesel = '01250811223'"));

        LocalDate pastVisitDay = workingDayFrom(today.minusDays(6));
        LocalDate nextVisitDay = workingDayFrom(today.plusDays(2));
        Random random = new Random(2026);
        Set<String> busyPatients = new HashSet<>();

        for (Map.Entry<String, LocalTime> entry : SCHEDULE.entrySet()) {
            long doctorId = findId(connection, "SELECT id FROM doctor WHERE license_number = '" + entry.getKey() + "'");
            for (LocalDate day = today.minusDays(7); !day.isAfter(today.plusDays(14)); day = day.plusDays(1)) {
                if (isWeekend(day)) {
                    continue;
                }
                for (int i = 0; i < SLOTS_PER_DAY; i++) {
                    LocalDateTime start = day.atTime(entry.getValue()).plusMinutes((long) i * SLOT_MINUTES);
                    LocalDateTime end = start.plusMinutes(SLOT_MINUTES);
                    boolean future = start.isAfter(now);

                    Long patientId = null;
                    String status = null;
                    String reason = null;
                    if (entry.getKey().equals("2345672") && day.equals(pastVisitDay) && i == 3) {
                        patientId = mateusz;
                        status = "COMPLETED";
                        reason = "Kontrola ciśnienia, przedłużenie recepty";
                    } else if (entry.getKey().equals("1234561") && day.equals(nextVisitDay) && i == 3) {
                        patientId = mateusz;
                        status = "BOOKED";
                        reason = "Konsultacja kardiologiczna - omówienie wyniku EKG";
                    } else if (entry.getKey().equals("3456783") && day.equals(nextVisitDay) && i == 6) {
                        patientId = joanna;
                        status = "BOOKED";
                        reason = "Szczepienie dziecka";
                    } else if (future && random.nextInt(100) < 25) {
                        patientId = otherPatients.get(random.nextInt(otherPatients.size()));
                        status = "BOOKED";
                        reason = "Wizyta kontrolna";
                    }
                    if (patientId != null && !busyPatients.add(patientId + "@" + start)) {
                        patientId = null;
                    }

                    long slotId = insertSlot(connection, doctorId, start, end, patientId != null);
                    if (patientId != null) {
                        insertAppointment(connection, slotId, patientId, status, reason, start.minusDays(3), null);
                    }
                    if (entry.getKey().equals("5678905") && day.equals(nextVisitDay) && i == 1) {
                        insertAppointment(connection, slotId, mateusz, "CANCELLED", "Konsultacja zmiany skórnej",
                                now.minusDays(2), now.minusDays(1));
                    }
                }
            }
        }
    }

    private static long insertSlot(Connection connection, long doctorId, LocalDateTime start, LocalDateTime end,
                                   boolean booked) throws SQLException {
        String sql = "INSERT INTO time_slot (doctor_id, start_time, end_time, booked, version) VALUES (?, ?, ?, ?, 0)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, doctorId);
            statement.setObject(2, start);
            statement.setObject(3, end);
            statement.setBoolean(4, booked);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private static void insertAppointment(Connection connection, long slotId, long patientId, String status,
                                          String reason, LocalDateTime createdAt, LocalDateTime cancelledAt)
            throws SQLException {
        String sql = "INSERT INTO appointment (slot_id, patient_id, status, reason, created_at, cancelled_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, slotId);
            statement.setLong(2, patientId);
            statement.setString(3, status);
            statement.setString(4, reason);
            statement.setObject(5, createdAt);
            statement.setObject(6, cancelledAt);
            statement.executeUpdate();
        }
    }

    private static long findId(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            if (!result.next()) {
                throw new IllegalStateException("Brak danych bazowych dla zapytania: " + sql);
            }
            return result.getLong(1);
        }
    }

    private static LocalDate workingDayFrom(LocalDate day) {
        LocalDate result = day;
        while (isWeekend(result)) {
            result = result.plusDays(1);
        }
        return result;
    }

    private static boolean isWeekend(LocalDate day) {
        return day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
