package pl.popielarz.rejestracja.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.popielarz.rejestracja.model.Patient;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    List<Patient> findByLastNameStartingWithIgnoreCaseOrderByLastNameAscFirstNameAsc(String lastName);

    List<Patient> findAllByOrderByLastNameAscFirstNameAsc();

    boolean existsByPesel(String pesel);

    boolean existsByEmailIgnoreCase(String email);
}
