package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
import cl.ufro.dci.naivepayapi.autentificacion.dto.LoginResponse;
import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import cl.ufro.dci.naivepayapi.dispositivos.service.DeviceService;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

/**
 * Service responsible for creating authenticated sessions.
 * Handles JWT generation, AuthAttempt logging, and Session persistence.
 */
@Service
@RequiredArgsConstructor
public class SessionCreator {

    private static final Logger logger = LoggerFactory.getLogger(SessionCreator.class);

    private final JWTService jwtService;
    private final AuthAttemptService authAttemptService;
    private final AuthSessionService authSessionService;
    private final DeviceService deviceService;

    /**
     * Creates a complete authenticated session: generates JWT token and persists session.
     * The device must already be validated before calling this method.
     * Follows the chain: Session -> AuthAttempt -> Device -> User
     *
     * @param user Authenticated user
     * @param deviceFingerprint Device fingerprint (must not be null, use empty string if unavailable)
     * @return LoginResponse with token, expiration, and session ID
     * @throws ResponseStatusException if device is not found (should never happen if validated before)
     */
    public LoginResponse createAuthenticatedSession(User user, String deviceFingerprint) {
        logger.debug("Creating authenticated session | userId={}", user.getUseId());

        // Generate JWT token with unique JTI
        UUID jti = UUID.randomUUID();
        String token = jwtService.generate(
                String.valueOf(user.getUseId()),
                deviceFingerprint,
                jti.toString()
        );
        Instant exp = jwtService.getExpiration(token);

        logger.debug("JWT token generated | userId={} | jti={} | expiration={}", user.getUseId(), jti, exp);

        // Obtain device (already validated previously)
        Device device = deviceService.findByUserId(user.getUseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Device should exist"));

        logger.debug("Device obtained | userId={} | fingerprint={}", user.getUseId(), device.getFingerprint());

        // 1. Create successful AuthAttempt
        var initialAuthAttempt = authAttemptService.log(device, true, AuthAttemptReason.OK);
        logger.debug("Initial AuthAttempt created | attemptId={}", initialAuthAttempt.getAttId());

        // 2. Create Session with initial AuthAttempt
        Session session = authSessionService.saveActiveSession(jti, initialAuthAttempt, exp);
        logger.debug("Session persisted | userId={} | sessionId={}", user.getUseId(), session.getSesId());

        // Build response
        return new LoginResponse(
                token,
                exp.toString(),
                session.getSesId().toString()
        );
    }
}
