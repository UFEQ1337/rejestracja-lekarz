package pl.popielarz.rejestracja.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.popielarz.rejestracja.model.Specialization;

import java.util.List;

public interface SpecializationRepository extends JpaRepository<Specialization, Long> {

    List<Specialization> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
