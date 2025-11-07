package cl.ufro.dci.naivepayapi.autentificacion.repository;

import cl.ufro.dci.naivepayapi.autentificacion.domain.AuthAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAttemptRepository extends JpaRepository<AuthAttempt, Long> {
}
