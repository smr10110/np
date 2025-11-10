package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.AuthAttempt;
import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.SessionStatus;
import cl.ufro.dci.naivepayapi.autentificacion.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio encargado de persistir y administrar las sesiones de autenticación
 * asociadas a tokens JWT emitidos por el sistema.
 * Follows the chain: Session -> AuthAttempt -> Device -> User
 */
@Service
public class AuthSessionService {

    private final SessionRepository authRepo;

    public AuthSessionService(SessionRepository authRepo) {
        this.authRepo = authRepo;
    }

    /**
     * Crea una nueva sesión activa basada en un intento de autenticación exitoso
     * @param jti JWT ID único para esta sesión
     * @param initialAuthAttempt El intento de autenticación que inició esta sesión
     * @param expiresAt Fecha de expiración de la sesión
     * @return La sesión creada
     */
    @Transactional
    public Session saveActiveSession(UUID jti, AuthAttempt initialAuthAttempt, Instant expiresAt) {
        Session auth = Session.builder()
                .sesJti(jti)
                .initialAuthAttempt(initialAuthAttempt)
                .sesCreated(Instant.now())
                .sesExpires(expiresAt)
                .status(SessionStatus.ACTIVE)
                .build();

        return authRepo.save(auth);
    }

    @Transactional(readOnly = true)
    public Optional<Session> findActiveByJti(UUID jti) {
        return authRepo.findBySesJtiAndStatus(jti, SessionStatus.ACTIVE);
    }

    @Transactional
    public Optional<Session> closeByJti(UUID jti) {
        return authRepo.findBySesJti(jti).map(a -> {
            if (a.getStatus() != SessionStatus.CLOSED) {
                a.setStatus(SessionStatus.CLOSED);
                if (a.getSesClosed() == null) {
                    Instant now = Instant.now();
                    Instant closedInstant = (a.getSesExpires() != null && now.isAfter(a.getSesExpires()))
                            ? a.getSesExpires()
                            : now;
                    a.setSesClosed(closedInstant);
                }
                return authRepo.save(a);
            }
            return a;
        });
    }
}