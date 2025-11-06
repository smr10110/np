package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.domain.SessionHistory;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.SessionStatus;
import cl.ufro.dci.naivepayapi.autentificacion.repository.SessionRepository;
import cl.ufro.dci.naivepayapi.autentificacion.repository.SessionHistoryRepository;
import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio encargado de persistir y administrar las sesiones de autenticación
 * asociadas a tokens JWT emitidos por el sistema.
 *
 * Usa 2 tablas:
 * - session: Solo sesiones ACTIVAS
 * - session_history: Sesiones cerradas (historial)
 */
@Service
public class AuthSessionService {

    private final SessionRepository authRepo;
    private final SessionHistoryRepository historyRepo;

    public AuthSessionService(SessionRepository authRepo, SessionHistoryRepository historyRepo) {
        this.authRepo = authRepo;
        this.historyRepo = historyRepo;
    }

    @Transactional
    public Session saveActiveSession(UUID jti, User user, Device device, Instant expiresAt) {
        Session auth = Session.builder()
                .sesJti(jti)
                .user(user)
                .device(device)
                .sesDeviceFingerprint(device != null ? device.getFingerprint() : null)
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

    /**
     * Cierra una sesión y la mueve a historial
     */
    @Transactional
    public void closeByJti(UUID jti, String reason) {
        authRepo.findBySesJti(jti).ifPresent(session -> {
            // 1. Guardar en historial
            SessionHistory history = session.toHistory(reason);
            historyRepo.save(history);

            // 2. Eliminar de tabla session
            authRepo.delete(session);

            System.out.println("✅ Sesión movida a historial: " + jti + " (razón: " + reason + ")");
        });
    }

    /**
     * Cierra todas las sesiones de un dispositivo y las mueve a historial
     */
    @Transactional
    public int closeSessionsByDeviceFingerprint(String fingerprint, String reason) {
        // Buscar sesiones con ese device
        List<Session> sessions = authRepo.findAll().stream()
                .filter(s -> s.getDevice() != null && fingerprint.equals(s.getDevice().getFingerprint()))
                .toList();

        // Mover cada una a historial
        sessions.forEach(session -> {
            SessionHistory history = session.toHistory(reason);
            historyRepo.save(history);
            authRepo.delete(session);
        });

        System.out.println("🔒 " + sessions.size() + " sesiones movidas a historial (razón: " + reason + ")");
        return sessions.size();
    }
}