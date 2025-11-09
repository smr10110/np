package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.AuthAttempt;
import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.autentificacion.repository.AuthAttemptRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthAttemptService {
    private final AuthAttemptRepository repo;
    public AuthAttemptService(AuthAttemptRepository repo) { this.repo = repo; }

    public void log(User user, String attDeviceFingerprint, Session session, boolean success, AuthAttemptReason reason) {
        var attempt = AuthAttempt.builder()
                .user(user)
                .attDeviceFingerprint(attDeviceFingerprint)
                .session(session)
                .attSuccess(success)
                .attReason(reason)
                .attOccurred(Instant.now())
                .build();
        repo.save(attempt);
    }

    /**
     * Cuenta los intentos fallidos de un usuario desde una fecha específica.
     *
     * @param userId ID del usuario
     * @param since Fecha desde la cual contar los intentos
     * @return Número de intentos fallidos desde la fecha indicada
     */
    public long countFailedAttemptsSince(Long userId, Instant since) {
        return repo.countFailedAttemptsSince(userId, since);
    }

    // exponer la última fecha/hora de intento exitoso para que AuthService pueda reiniciar el contador tras un login correcto.
    public Instant findLastSuccessAt(Long userId) {
        return repo.findLastSuccessAt(userId);
    }
}