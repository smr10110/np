package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.AuthAttempt;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import cl.ufro.dci.naivepayapi.autentificacion.repository.AuthAttemptRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for managing authentication attempts
 * */
@Service
public class AuthAttemptService {
    private final AuthAttemptRepository repo;
    public AuthAttemptService(AuthAttemptRepository repo) { this.repo = repo; }

    /**
     * Registra un intento de autenticación
     * @param device Device que realizó el intento
     * @param success Si el intento fue exitoso
     * @param reason Razón del intento
     * @return El AuthAttempt creado
     */
    public AuthAttempt log(Device device, boolean success, AuthAttemptReason reason) {
        var attempt = AuthAttempt.builder()
                .device(device)
                .attSuccess(success)
                .attReason(reason)
                .attOccurred(Instant.now())
                .build();
        return repo.save(attempt);
    }
}