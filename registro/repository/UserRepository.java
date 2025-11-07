package cl.ufro.dci.naivepayapi.registro.repository;

import cl.ufro.dci.naivepayapi.registro.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUseRutGeneral(Long useRutGeneral);

    Optional<User> findByRegisterRegEmail(String regEmail);

    Optional<User> findByUseRutGeneralAndUseVerificationDigit(Long useRutGeneral, char useVerificationDigit);
}
