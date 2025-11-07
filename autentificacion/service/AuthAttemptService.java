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
        var a = new AuthAttempt();
        a.setUser(user);
        a.setAttDeviceFingerprint(attDeviceFingerprint);
        a.setSession(session);
        a.setAttSuccess(success);
        a.setAttReason(reason);
        a.setAttOccurred(Instant.now());
        repo.save(a);
    }
}