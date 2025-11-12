package cl.ufro.dci.naivepayapi.autentificacion.util;

import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * Utility class for building standardized login response entities.
 * Centralizes response construction to ensure consistency across authentication endpoints.
 */
public final class LoginResponseBuilder {

    // Private constructor to prevent instantiation
    private LoginResponseBuilder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Builds a 401 Unauthorized response with the given reason.
     *
     * @param reason Reason for the unauthorized response
     * @return ResponseEntity with 401 status and error body
     */
    public static ResponseEntity<Map<String, Object>> unauthorized(AuthAttemptReason reason) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", reason.name()));
    }

    /**
     * Builds a 403 Forbidden response with the given reason.
     *
     * @param reason Reason for the forbidden response
     * @return ResponseEntity with 403 status and error body
     */
    public static ResponseEntity<Map<String, Object>> forbidden(AuthAttemptReason reason) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", reason.name()));
    }

    /**
     * Builds a 401 Unauthorized response for bad credentials with remaining attempts count.
     *
     * @param remainingAttempts Number of login attempts remaining before account lockout
     * @return ResponseEntity with 401 status, error code, and remaining attempts
     */
    public static ResponseEntity<Map<String, Object>> badCredentialsWithAttempts(int remainingAttempts) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "error", AuthAttemptReason.BAD_CREDENTIALS.name(),
                        "remainingAttempts", remainingAttempts
                ));
    }

    /**
     * Builds a generic 401 Unauthorized response (used for logout errors).
     *
     * @return ResponseEntity with 401 status and generic error message
     */
    public static ResponseEntity<Map<String, Object>> unauthorizedGeneric() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "UNAUTHORIZED"));
    }
}
