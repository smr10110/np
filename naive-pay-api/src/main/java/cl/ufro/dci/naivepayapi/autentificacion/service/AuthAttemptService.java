package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.AuthAttempt;
import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.autentificacion.repository.AuthAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthAttemptService {
    private final AuthAttemptRepository repo;

    // Registra un intento de autenticación (exitoso o fallido)
    @Transactional
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

    // Cuenta los intentos fallidos desde una fecha específica
    @Transactional(readOnly = true)
    public long countFailedAttemptsSince(Long userId, Instant since) {
        return repo.countFailedAttemptsSince(userId, since);
    }

    // Obtiene la fecha del último intento exitoso (para reiniciar contador)
    @Transactional(readOnly = true)
    public Instant findLastSuccessAt(Long userId) {
        return repo.findLastSuccessAt(userId);
    }

    // Registra un reseteo de contraseña exitoso (resetea el contador de intentos fallidos)
    @Transactional
    public void logPasswordResetAsSuccess(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        var attempt = AuthAttempt.builder()
                .user(user)
                .attDeviceFingerprint(null)  // No hay dispositivo en el reseteo
                .session(null)                // No crea sesión activa
                .attSuccess(true)             // Evento EXITOSO - resetea contador
                .attReason(AuthAttemptReason.PASSWORD_RESET)
                .attOccurred(Instant.now())
                .build();

        repo.save(attempt);
    }
}