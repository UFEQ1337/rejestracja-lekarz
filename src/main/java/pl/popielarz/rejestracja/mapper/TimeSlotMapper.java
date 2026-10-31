package pl.popielarz.rejestracja.mapper;

import org.springframework.stereotype.Component;
import pl.popielarz.rejestracja.dto.TimeSlotDto;
import pl.popielarz.rejestracja.model.TimeSlot;

@Component
public class TimeSlotMapper {

    public TimeSlotDto.Response toResponse(TimeSlot slot) {
        return new TimeSlotDto.Response(
                slot.getId(),
                slot.getDoctor().getId(),
                slot.getDoctor().getFullName(),
                slot.getStartTime(),
                slot.getEndTime(),
                !slot.isBooked());
    }
}
