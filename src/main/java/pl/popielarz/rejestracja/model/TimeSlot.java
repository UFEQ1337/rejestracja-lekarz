package pl.popielarz.rejestracja.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * Termin przyjęć lekarza. Pole {@code version} chroni przed sytuacją, w której
 * dwóch pacjentów jednocześnie rezerwuje ten sam termin (optimistic locking).
 */
@Entity
@Table(name = "time_slot")
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private boolean booked;

    @Version
    private long version;

    protected TimeSlot() {
    }

    public TimeSlot(Doctor doctor, LocalDateTime startTime, LocalDateTime endTime) {
        this.doctor = doctor;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean startsBefore(LocalDateTime moment) {
        return startTime.isBefore(moment);
    }

    public void markBooked() {
        this.booked = true;
    }

    public void release() {
        this.booked = false;
    }

    public Long getId() {
        return id;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public boolean isBooked() {
        return booked;
    }

    public long getVersion() {
        return version;
    }
}
