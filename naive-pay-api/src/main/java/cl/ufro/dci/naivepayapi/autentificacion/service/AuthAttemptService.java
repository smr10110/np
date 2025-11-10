package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.AuthAttempt;
import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import cl.ufro.dci.naivepayapi.autentificacion.repository.AuthAttemptRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for managing authentication attempts
 * Follows the chain: Session -> AuthAttempt -> Device -> User
 */
@Service
public class AuthAttemptService {
    private final AuthAttemptRepository repo;
    public AuthAttemptService(AuthAttemptRepository repo) { this.repo = repo; }

    /**
     * Registra un intento de autenticación
     * @param device Device que realizó el intento
     * @param session Sesión asociada (puede ser null si el intento falló)
     * @param success Si el intento fue exitoso
     * @param reason Razón del intento
     * @return El AuthAttempt creado
     */
    public AuthAttempt log(Device device, Session session, boolean success, AuthAttemptReason reason) {
        var attempt = AuthAttempt.builder()
                .device(device)
                .session(session)
                .attSuccess(success)
                .attReason(reason)
                .attOccurred(Instant.now())
                .build();
        return repo.save(attempt);
    }

    /**
     * Cuenta los intentos fallidos de un usuario desde una fecha específica.
     * Navega a través de: AuthAttempt -> Device -> User
     *
     * @param userId ID del usuario
     * @param since Fecha desde la cual contar los intentos
     * @return Número de intentos fallidos desde la fecha indicada
     */
    public long countFailedAttemptsSince(Long userId, Instant since) {
        return repo.countFailedAttemptsSince(userId, since);
    }

    /**
     * Expone la última fecha/hora de intento exitoso para que AuthService pueda reiniciar el contador tras un login correcto.
     * Navega a través de: AuthAttempt -> Device -> User
     */
    public Instant findLastSuccessAt(Long userId) {
        return repo.findLastSuccessAt(userId);
    }
}