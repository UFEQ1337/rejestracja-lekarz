package pl.popielarz.rejestracja.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.popielarz.rejestracja.model.Doctor;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @EntityGraph(attributePaths = "specializations")
    @Query("""
            select distinct d from Doctor d
            left join d.specializations s
            where d.active = true
              and (:specializationId is null or s.id = :specializationId)
            order by d.lastName, d.firstName
            """)
    List<Doctor> findActive(@Param("specializationId") Long specializationId);

    @EntityGraph(attributePaths = "specializations")
    Optional<Doctor> findByIdAndActiveTrue(Long id);

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByEmailIgnoreCase(String email);
}
