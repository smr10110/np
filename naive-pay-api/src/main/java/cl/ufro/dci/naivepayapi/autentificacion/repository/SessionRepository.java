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

    // NOTA: Con el nuevo modelo Session -> AuthAttempt -> Device -> User,
    // Session ya no tiene relación directa con Device.
    // Si se necesita desvincular dispositivos, debe hacerse a nivel de AuthAttempt.
    // El método detachDeviceByFingerprint ha sido removido ya que no es compatible con el nuevo modelo.
}
