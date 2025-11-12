package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
import cl.ufro.dci.naivepayapi.autentificacion.dto.LoginRequest;
import cl.ufro.dci.naivepayapi.autentificacion.dto.LoginResponse;
import cl.ufro.dci.naivepayapi.autentificacion.util.BearerTokenUtil;
import cl.ufro.dci.naivepayapi.autentificacion.util.FingerprintSanitizer;
import cl.ufro.dci.naivepayapi.autentificacion.util.LoginResponseBuilder;
import cl.ufro.dci.naivepayapi.dispositivos.service.DeviceService;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Core authentication service orchestrating the login/logout flow.
 * Delegates specific responsibilities to specialized services for maintainability.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // Core dependencies
    private final JWTService jwtService;
    private final AuthAttemptService authAttemptService;
    private final AuthSessionService authSessionService;
    private final DeviceService deviceService;

    // Refactored services
    private final UserResolver userResolver;
    private final LoginValidator loginValidator;
    private final SessionCreator sessionCreator;
    private final AccountLockService accountLockService;

    // =========================== LOGIN ===========================

    /**
     * Authenticates a user with their credentials and device fingerprint.
     *
     * Login flow:
     * 1. Validate input credentials
     * 2. Resolve user by identifier (email or RUT)
     * 3. Validate preconditions (email verified, account not locked)
     * 4. Validate device authorization
     * 5. Validate password
     * 6. Create authenticated session
     *
     * @param req Login request with identifier and password
     * @param deviceFingerprint Device fingerprint for multi-factor authentication
     * @return ResponseEntity with LoginResponse on success, or error details on failure
     */
    public ResponseEntity<?> login(LoginRequest req, String deviceFingerprint) {
        MDC.put("identifier", req.getIdentifier());
        MDC.put("deviceFingerprint", deviceFingerprint != null ? deviceFingerprint : "N/A");

        try {
            logger.debug("Login attempt received | identifier={}", req.getIdentifier());

            // Step 1: Validate input
            try {
                loginValidator.validateCredentialsNotBlank(req.getIdentifier(), req.getPassword());
            } catch (ResponseStatusException ex) {
                return LoginResponseBuilder.unauthorized(AuthAttemptReason.valueOf(ex.getReason()));
            }

            // Step 2: Resolve user
            Optional<User> userOpt = userResolver.resolve(req.getIdentifier());
            if (userOpt.isEmpty()) {
                logger.warn("Login rejected: user not found | identifier={}", req.getIdentifier());
                return LoginResponseBuilder.unauthorized(AuthAttemptReason.USER_NOT_FOUND);
            }
            User user = userOpt.get();

            MDC.put("userId", String.valueOf(user.getUseId()));
            MDC.put("email", user.getRegister().getRegEmail());

            // Step 3: Validate preconditions
            try {
                loginValidator.validateEmailVerified(user);
                loginValidator.validateAccountNotLocked(user);
            } catch (ResponseStatusException ex) {
                logFailedAttempt(user, AuthAttemptReason.valueOf(ex.getReason()));
                return ex.getStatusCode().equals(HttpStatus.FORBIDDEN)
                        ? LoginResponseBuilder.forbidden(AuthAttemptReason.valueOf(ex.getReason()))
                        : LoginResponseBuilder.unauthorized(AuthAttemptReason.valueOf(ex.getReason()));
            }

            // Step 4: Validate device authorization (before password check)
            String safeFingerprint = FingerprintSanitizer.sanitize(deviceFingerprint);
            try {
                deviceService.ensureAuthorizedDevice(user.getUseId(), safeFingerprint);
                logger.debug("Device verified | userId={}", user.getUseId());
            } catch (ResponseStatusException ex) {
                return handleDeviceAuthorizationError(user, ex);
            }

            // Step 5: Validate password
            if (!loginValidator.isPasswordValid(user, req.getPassword())) {
                return handleInvalidPassword(user);
            }

            // Step 6: Create authenticated session
            LoginResponse response = sessionCreator.createAuthenticatedSession(user, safeFingerprint);
            logger.info("Login successful | userId={} | email={} | jti={}",
                    user.getUseId(), user.getRegister().getRegEmail(), response.getJti());
            return ResponseEntity.ok(response);

        } finally {
            MDC.remove("identifier");
            MDC.remove("deviceFingerprint");
            MDC.remove("userId");
            MDC.remove("email");
        }
    }

    // =========================== LOGOUT ===========================

    /**
     * Closes the current user session by invalidating the JWT token.
     *
     * @param authHeader Authorization header with Bearer token (e.g., "Bearer eyJhbGci...")
     * @return ResponseEntity with success message (200) or authentication error (401)
     */
    public ResponseEntity<Map<String, Object>> logout(String authHeader) {
        try {
            logger.debug("Logout request received");

            String token = BearerTokenUtil.extractToken(authHeader);
            if (token == null) {
                logger.warn("Logout rejected: token absent or invalid");
                return LoginResponseBuilder.unauthorizedGeneric();
            }

            try {
                UUID jti = UUID.fromString(jwtService.getJti(token));
                MDC.put("jti", jti.toString());

                authSessionService.closeByJti(jti);

                logger.info("Logout successful | jti={}", jti);
                return ResponseEntity.ok(Map.of("message", "Sesión cerrada", "jti", jti));
            } catch (JwtException | IllegalArgumentException ex) {
                logger.warn("Logout rejected: invalid JWT token | error={}", ex.getMessage());
                return LoginResponseBuilder.unauthorizedGeneric();
            } finally {
                MDC.remove("jti");
            }
        } catch (Exception ex) {
            logger.error("Unexpected error during logout | error={}", ex.getMessage(), ex);
            return LoginResponseBuilder.unauthorizedGeneric();
        }
    }

    // =========================== PRIVATE HELPERS ===========================

    /**
     * Handles invalid password attempt, managing account lockout and remaining attempts.
     *
     * @param user User with invalid password
     * @return ResponseEntity with appropriate error response
     */
    private ResponseEntity<?> handleInvalidPassword(User user) {
        logger.warn("Login rejected: invalid credentials | userId={}", user.getUseId());
        logFailedAttempt(user, AuthAttemptReason.BAD_CREDENTIALS);

        // Check if account should be blocked after this failed attempt
        boolean wasBlocked = accountLockService.checkAndBlockIfNeeded(user);

        if (wasBlocked) {
            logger.warn("Account auto-blocked after failed attempt | userId={}", user.getUseId());
            return LoginResponseBuilder.forbidden(AuthAttemptReason.ACCOUNT_BLOCKED);
        }

        // Calculate remaining attempts
        int remainingAttempts = accountLockService.calculateRemainingAttempts(user);
        logger.debug("Remaining attempts: {} | userId={}", remainingAttempts, user.getUseId());

        if (remainingAttempts == 0) {
            logger.warn("Blocking account: attempts exhausted | userId={}", user.getUseId());
            accountLockService.blockAccount(user);
            logFailedAttempt(user, AuthAttemptReason.ACCOUNT_BLOCKED);
            return LoginResponseBuilder.forbidden(AuthAttemptReason.ACCOUNT_BLOCKED);
        }

        return LoginResponseBuilder.badCredentialsWithAttempts(remainingAttempts);
    }

    /**
     * Handles device authorization errors during login.
     *
     * @param user User attempting to authenticate
     * @param ex Exception thrown by deviceService
     * @return ResponseEntity with 403 status and error reason
     * @throws ResponseStatusException if the error is not a device authorization error
     */
    private ResponseEntity<?> handleDeviceAuthorizationError(User user, ResponseStatusException ex) {
        if (!ex.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
            throw ex;
        }

        String reason = ex.getReason();

        if (AuthAttemptReason.DEVICE_REQUIRED.name().equals(reason)) {
            logFailedAttempt(user, AuthAttemptReason.DEVICE_REQUIRED);
            return LoginResponseBuilder.forbidden(AuthAttemptReason.DEVICE_REQUIRED);
        }

        if (AuthAttemptReason.DEVICE_UNAUTHORIZED.name().equals(reason)) {
            logFailedAttempt(user, AuthAttemptReason.DEVICE_UNAUTHORIZED);
            return LoginResponseBuilder.forbidden(AuthAttemptReason.DEVICE_UNAUTHORIZED);
        }

        throw ex;
    }

    /**
     * Logs a failed authentication attempt.
     * Automatically retrieves the user's device if available.
     *
     * @param user User who had the failed attempt
     * @param reason Reason for the failed attempt
     */
    private void logFailedAttempt(User user, AuthAttemptReason reason) {
        deviceService.findByUserId(user.getUseId())
                .ifPresent(dev -> authAttemptService.log(dev, false, reason));
    }
}
