package pl.popielarz.rejestracja.mapper;

import org.springframework.stereotype.Component;
import pl.popielarz.rejestracja.dto.AppointmentDto;
import pl.popielarz.rejestracja.model.Appointment;
import pl.popielarz.rejestracja.model.Doctor;
import pl.popielarz.rejestracja.model.TimeSlot;

@Component
public class AppointmentMapper {

    public AppointmentDto.Response toResponse(Appointment appointment) {
        TimeSlot slot = appointment.getSlot();
        Doctor doctor = slot.getDoctor();
        return new AppointmentDto.Response(
                appointment.getId(),
                appointment.getStatus(),
                appointment.getReason(),
                appointment.getCreatedAt(),
                appointment.getCancelledAt(),
                slot.getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                doctor.getId(),
                doctor.getFullName(),
                doctor.getRoom(),
                appointment.getPatient().getId(),
                appointment.getPatient().getFullName());
    }
}
