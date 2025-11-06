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

    // Cierra todas las sesiones activas asociadas a un dispositivo específico
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE session SET ses_status = 'CLOSED', ses_closed = CURRENT_TIMESTAMP " +
            "WHERE dev_fingerprint = :fp AND ses_status = 'ACTIVE'",
            nativeQuery = true)
    int closeSessionsByDeviceFingerprint(@Param("fp") String fingerprint);

    // Libera la referencia a Device en sesiones por fingerprint (mitigación histórica)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Session s set s.device = null where s.device.fingerprint = :fp")
    int detachDeviceByFingerprint(@Param("fp") String fingerprint);
}
