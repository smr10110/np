package cl.ufro.dci.naivepayapi.autentificacion.repository;

import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findBySesJtiAndStatus(UUID sesJti, SessionStatus status);

    Optional<Session> findBySesJti(UUID sesJti);

    // Libera la referencia a Device en sesiones por fingerprint (mitigación histórica)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Session s set s.device = null where s.device.fingerprint = :fp")
    int detachDeviceByFingerprint(@Param("fp") String fingerprint);
}
