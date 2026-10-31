package pl.popielarz.rejestracja.mapper;

import org.springframework.stereotype.Component;
import pl.popielarz.rejestracja.dto.PatientDto;
import pl.popielarz.rejestracja.model.Patient;

@Component
public class PatientMapper {

    public PatientDto.Response toResponse(Patient patient) {
        return new PatientDto.Response(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getFullName(),
                patient.getPesel(),
                patient.getEmail(),
                patient.getPhone(),
                patient.getBirthDate());
    }

    public Patient toEntity(PatientDto.Request request) {
        Patient patient = new Patient(request.firstName(), request.lastName(),
                request.pesel(), request.email());
        patient.setPhone(request.phone());
        patient.setBirthDate(request.birthDate());
        return patient;
    }

    public void updateEntity(Patient patient, PatientDto.Request request) {
        patient.setFirstName(request.firstName());
        patient.setLastName(request.lastName());
        patient.setPesel(request.pesel());
        patient.setEmail(request.email());
        patient.setPhone(request.phone());
        patient.setBirthDate(request.birthDate());
    }
}
