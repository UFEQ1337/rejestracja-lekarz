package pl.popielarz.rejestracja.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.popielarz.rejestracja.model.Appointment;
import pl.popielarz.rejestracja.model.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(attributePaths = {"slot", "slot.doctor", "patient"})
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findWithDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"slot", "slot.doctor", "patient"})
    @Query("""
            select a from Appointment a
            where (:patientId is null or a.patient.id = :patientId)
              and (:doctorId is null or a.slot.doctor.id = :doctorId)
              and (:status is null or a.status = :status)
              and a.slot.startTime >= :from and a.slot.startTime < :to
            order by a.slot.startTime
            """)
    List<Appointment> search(@Param("patientId") Long patientId,
                             @Param("doctorId") Long doctorId,
                             @Param("status") AppointmentStatus status,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to);

    @Query("""
            select count(a) > 0 from Appointment a
            where a.patient.id = :patientId
              and a.status = pl.popielarz.rejestracja.model.AppointmentStatus.BOOKED
              and a.slot.startTime < :end and a.slot.endTime > :start
            """)
    boolean patientHasAppointmentBetween(@Param("patientId") Long patientId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);
}
