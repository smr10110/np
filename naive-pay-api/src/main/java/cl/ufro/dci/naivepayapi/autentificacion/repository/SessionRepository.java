package cl.ufro.dci.naivepayapi.autentificacion.repository;

import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, Long> {

    @Query("SELECT s FROM Session s " +
           "LEFT JOIN FETCH s.initialAuthAttempt a " +
           "LEFT JOIN FETCH a.device d " +
           "LEFT JOIN FETCH d.user " +
           "WHERE s.sesJti = :sesJti AND s.status = :status")
    Optional<Session> findBySesJtiAndStatus(@Param("sesJti") UUID sesJti, @Param("status") SessionStatus status);

    @Query("SELECT s FROM Session s " +
           "LEFT JOIN FETCH s.initialAuthAttempt a " +
           "LEFT JOIN FETCH a.device d " +
           "LEFT JOIN FETCH d.user " +
           "WHERE s.sesJti = :sesJti")
    Optional<Session> findBySesJti(@Param("sesJti") UUID sesJti);

    // NOTA: Con el nuevo modelo Session -> AuthAttempt -> Device -> User,
    // Session ya no tiene relación directa con Device.
    // Si se necesita desvincular dispositivos, debe hacerse a nivel de AuthAttempt.
    // El método detachDeviceByFingerprint ha sido removido ya que no es compatible con el nuevo modelo.

    /**
     * Encuentra sesiones que tienen att_id_initial null (sesiones antiguas antes de la refactorización)
     * Útil para identificar y limpiar datos inconsistentes
     */
    @Query("SELECT s FROM Session s WHERE s.initialAuthAttempt IS NULL")
    List<Session> findOrphanedSessions();

    /**
     * Cierra todas las sesiones huérfanas (sin AuthAttempt inicial válido)
     */
    @Modifying
    @Query("UPDATE Session s SET s.status = 'CLOSED', s.sesClosed = CURRENT_TIMESTAMP " +
           "WHERE s.initialAuthAttempt IS NULL AND s.status <> 'CLOSED'")
    int closeOrphanedSessions();
}
