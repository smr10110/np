package cl.ufro.dci.naivepayapi.autentificacion.repository;

import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para sesiones ACTIVAS.
 * Las sesiones cerradas se mueven a session_history.
 */
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findBySesJtiAndStatus(UUID sesJti, SessionStatus status);

    Optional<Session> findBySesJti(UUID sesJti);
}
