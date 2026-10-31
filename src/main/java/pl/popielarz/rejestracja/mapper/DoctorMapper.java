package pl.popielarz.rejestracja.mapper;

import org.springframework.stereotype.Component;
import pl.popielarz.rejestracja.dto.DoctorDto;
import pl.popielarz.rejestracja.model.Doctor;

@Component
public class DoctorMapper {

    private final SpecializationMapper specializationMapper;

    public DoctorMapper(SpecializationMapper specializationMapper) {
        this.specializationMapper = specializationMapper;
    }

    public DoctorDto.Response toResponse(Doctor doctor) {
        return new DoctorDto.Response(
                doctor.getId(),
                doctor.getFirstName(),
                doctor.getLastName(),
                doctor.getFullName(),
                doctor.getLicenseNumber(),
                doctor.getEmail(),
                doctor.getPhone(),
                doctor.getRoom(),
                doctor.getSpecializations().stream()
                        .map(specializationMapper::toResponse)
                        .toList());
    }

    public Doctor toEntity(DoctorDto.Request request) {
        Doctor doctor = new Doctor(request.firstName(), request.lastName(),
                request.licenseNumber(), request.email());
        doctor.setPhone(request.phone());
        doctor.setRoom(request.room());
        return doctor;
    }

    public void updateEntity(Doctor doctor, DoctorDto.Request request) {
        doctor.setFirstName(request.firstName());
        doctor.setLastName(request.lastName());
        doctor.setLicenseNumber(request.licenseNumber());
        doctor.setEmail(request.email());
        doctor.setPhone(request.phone());
        doctor.setRoom(request.room());
    }
}
