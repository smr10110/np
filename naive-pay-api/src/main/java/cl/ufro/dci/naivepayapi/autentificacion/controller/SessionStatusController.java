package cl.ufro.dci.naivepayapi.autentificacion.controller;

import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.SessionStatus;
import cl.ufro.dci.naivepayapi.autentificacion.repository.SessionRepository;
import cl.ufro.dci.naivepayapi.autentificacion.service.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SessionStatusController {
    private final JWTService jwtService;
    private final SessionRepository sessionRepository;

    @Value("${security.session.inactivity-timeout-minutes:10}")
    private long inactivityTimeoutMinutes;

    @GetMapping("/session-status")
    public ResponseEntity<SessionStatusResponse> getSessionStatus(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.replace("Bearer ", "");
        String jtiStr = jwtService.getJti(token);
        UUID jti = UUID.fromString(jtiStr);

        Session session = sessionRepository.findBySesJtiAndStatus(jti, SessionStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SESSION_NOT_FOUND"));

        Instant now = Instant.now();

        // Calcular minutos restantes hasta timeout de inactividad
        long minutesSinceActivity = ChronoUnit.MINUTES.between(session.getSesLastActivity(), now);
        long minutesUntilInactivity = inactivityTimeoutMinutes - minutesSinceActivity;

        // Calcular minutos restantes hasta expiración absoluta
        long minutesUntilMaxExpiration = ChronoUnit.MINUTES.between(now, session.getSesMaxExpiration());

        // Devolver el límite más restrictivo (el que expire primero)
        long minutesRemaining = Math.min(minutesUntilInactivity, minutesUntilMaxExpiration);

        return ResponseEntity.ok(new SessionStatusResponse(
                Math.max(0, minutesRemaining),
                Math.max(0, minutesUntilInactivity),
                Math.max(0, minutesUntilMaxExpiration)
        ));
    }
}

record SessionStatusResponse(
        long minutesRemaining,
        long minutesUntilInactivity,
        long minutesUntilMaxExpiration
) {}
