package pl.popielarz.rejestracja.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private TimeSlot slot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    protected Appointment() {
    }

    public Appointment(TimeSlot slot, Patient patient, String reason, LocalDateTime createdAt) {
        this.slot = slot;
        this.patient = patient;
        this.reason = reason;
        this.createdAt = createdAt;
        this.status = AppointmentStatus.BOOKED;
    }

    public void cancel(LocalDateTime when) {
        this.status = AppointmentStatus.CANCELLED;
        this.cancelledAt = when;
        slot.release();
    }

    public void complete() {
        this.status = AppointmentStatus.COMPLETED;
    }

    public boolean isBooked() {
        return status == AppointmentStatus.BOOKED;
    }

    public Long getId() {
        return id;
    }

    public TimeSlot getSlot() {
        return slot;
    }

    public Patient getPatient() {
        return patient;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
