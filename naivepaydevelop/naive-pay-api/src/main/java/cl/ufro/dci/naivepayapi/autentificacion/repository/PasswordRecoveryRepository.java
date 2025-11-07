package cl.ufro.dci.naivepayapi.autentificacion.repository;

import cl.ufro.dci.naivepayapi.autentificacion.domain.PasswordRecovery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordRecoveryRepository extends JpaRepository<PasswordRecovery, Long> {
}
