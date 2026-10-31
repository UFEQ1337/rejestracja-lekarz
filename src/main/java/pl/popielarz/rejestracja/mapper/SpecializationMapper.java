package pl.popielarz.rejestracja.mapper;

import org.springframework.stereotype.Component;
import pl.popielarz.rejestracja.dto.SpecializationDto;
import pl.popielarz.rejestracja.model.Specialization;

@Component
public class SpecializationMapper {

    public SpecializationDto.Response toResponse(Specialization specialization) {
        return new SpecializationDto.Response(specialization.getId(), specialization.getName());
    }

    public Specialization toEntity(SpecializationDto.Request request) {
        return new Specialization(request.name().trim());
    }
}
