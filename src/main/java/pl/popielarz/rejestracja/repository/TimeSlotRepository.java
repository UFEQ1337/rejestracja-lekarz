package pl.popielarz.rejestracja.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.popielarz.rejestracja.model.TimeSlot;

import java.time.LocalDateTime;
import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    @Query("""
            select t from TimeSlot t
            where t.doctor.id = :doctorId
              and t.startTime >= :from and t.startTime < :to
              and (:onlyAvailable = false or t.booked = false)
            order by t.startTime
            """)
    List<TimeSlot> findForDoctor(@Param("doctorId") Long doctorId,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to,
                                 @Param("onlyAvailable") boolean onlyAvailable);

    @Query("""
            select count(t) > 0 from TimeSlot t
            where t.doctor.id = :doctorId
              and t.startTime < :end and t.endTime > :start
            """)
    boolean existsOverlapping(@Param("doctorId") Long doctorId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    boolean existsByDoctorIdAndBookedTrueAndStartTimeAfter(Long doctorId, LocalDateTime moment);
}
