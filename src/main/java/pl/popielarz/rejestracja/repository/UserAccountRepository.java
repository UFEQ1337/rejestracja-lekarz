package pl.popielarz.rejestracja.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.popielarz.rejestracja.model.UserAccount;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);
}
