package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.SessionStatus;
import cl.ufro.dci.naivepayapi.autentificacion.repository.SessionRepository;
import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio encargado de persistir y administrar las sesiones de autenticación
 * asociadas a tokens JWT emitidos por el sistema.
 */
@Service
public class AuthSessionService {

    private final SessionRepository authRepo;

    @Value("${security.session.inactivity-timeout-minutes:10}")
    private long inactivityTimeoutMinutes;

    @Value("${security.session.max-session-lifetime-minutes:30}")
    private long maxSessionLifetimeMinutes;

    public AuthSessionService(SessionRepository authRepo) {
        this.authRepo = authRepo;
    }

    @Transactional
    public Session saveActiveSession(UUID jti, User user, Device device, Instant expiresAt) {
        Instant now = Instant.now();
        Instant maxExpiration = now.plus(maxSessionLifetimeMinutes, ChronoUnit.MINUTES);

        Session auth = Session.builder()
                .sesJti(jti)
                .user(user)
                .device(device)
                .sesDeviceFingerprint(device != null ? device.getFingerprint() : null)
                .sesCreated(now)
                .sesExpires(expiresAt)
                .sesLastActivity(now)
                .sesMaxExpiration(maxExpiration)
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

    @Transactional
    public void updateLastActivity(UUID jti) {
        Session session = authRepo.findBySesJtiAndStatus(jti, SessionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        Instant now = Instant.now();
        Instant lastUpdate = session.getSesLastActivity();

        // Validar límite absoluto de sesión (30 min desde login)
        if (now.isAfter(session.getSesMaxExpiration())) {
            session.setStatus(SessionStatus.CLOSED);
            session.setSesClosed(session.getSesMaxExpiration());
            authRepo.save(session);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED");
        }

        // Optimización: solo actualizar si pasó más de 1 minuto desde última actualización
        if (ChronoUnit.MINUTES.between(lastUpdate, now) < 1) {
            return;
        }

        // Validar que no haya superado tiempo de inactividad (10 min)
        Instant inactivityLimit = lastUpdate.plus(inactivityTimeoutMinutes, ChronoUnit.MINUTES);
        if (now.isAfter(inactivityLimit)) {
            session.setStatus(SessionStatus.CLOSED);
            session.setSesClosed(inactivityLimit);
            authRepo.save(session);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SESSION_INACTIVE");
        }

        // Actualizar última actividad
        session.setSesLastActivity(now);
        authRepo.save(session);
    }
}