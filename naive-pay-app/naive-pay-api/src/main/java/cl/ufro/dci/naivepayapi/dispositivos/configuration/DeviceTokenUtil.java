package cl.ufro.dci.naivepayapi.dispositivos.configuration;

import cl.ufro.dci.naivepayapi.autentificacion.service.JWTService;
import cl.ufro.dci.naivepayapi.dispositivos.repository.DeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Utilities for JWT and device fingerprint.
 * Responsibilities:
 * - Extract a Bearer token from Authorization header
 * - Validate JWT structure and expiration
 * - Read claims (userId, device fingerprint) from a JWT
 * - Resolve fingerprint from JWT or "X-Device-Fingerprint" header
 */
@Component
@RequiredArgsConstructor
public class DeviceTokenUtil {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String FINGERPRINT_HEADER = "X-Device-Fingerprint";

    private final JWTService jwtService;
    private final DeviceRepository deviceRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Extracts a raw JWT token from an Authorization header value.
     * Expected format: Bearer <token>
     *
     * Example (input header):
     *   Authorization: Bearer eyJhbGciOiJIUzM4NCJ9.eyJqdGkiOiI5MzI5Yi...
     *
     * Example (return):
     *   eyJhbGciOiJIUzM4NCJ9.eyJqdGkiOiI5MzI5Yi...
     *
     * Throws when:
     * - Header missing or blank
     * - Header does not start with "Bearer "
     * - Token part is empty after the prefix
     *
     * @param authorizationHeaderValue value of the Authorization header
     * @return raw JWT token without the Bearer prefix
     * @throws IllegalArgumentException if header is missing, blank or invalid
     */
    public String extractBearerTokenFromHeader(String authorizationHeaderValue) {
        if (authorizationHeaderValue == null || authorizationHeaderValue.isBlank()) {
            throw new IllegalArgumentException("Missing Authorization header");
        }
        if (!authorizationHeaderValue.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Invalid Authorization format (expected: 'Bearer <token>')");
        }
        return authorizationHeaderValue.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * Reads the Authorization header from the request and extracts the raw JWT.
     *
     * @param request HTTP request
     * @return raw JWT token without the Bearer prefix
     * @throws IllegalArgumentException if header is missing or invalid
     */
    public String extractBearerTokenFromRequest(HttpServletRequest request) {
        return extractBearerTokenFromHeader(request.getHeader(AUTHORIZATION_HEADER));
    }

    /**
     * Validates the JWT by parsing it and ensuring it has not expired.
     *
     * @param bearerToken raw JWT token
     * @throws IllegalArgumentException if the token is invalid or expired
     */
    public void validateTokenNotExpired(String bearerToken) {
        jwtService.parse(bearerToken);
        if (jwtService.isExpired(bearerToken)) {
            throw new IllegalArgumentException("Token expired");
        }
    }

    // ======= UserId methods

    /**
     * Extracts the userId claim from a raw JWT
     *
     * @param token raw JWT token
     * @return userId as Long
     * @throws IllegalArgumentException if token invalid/expired or claim missing/non-numeric
     */
    public Long extractUserIdFromJwt(String token) {
        validateTokenNotExpired(token);
        final String userId = jwtService.getUserId(token);

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Token does not contain userId claim");
        }
        return Long.valueOf(userId);
    }

    /**
     * Reads Authorization from the request and extracts the userId from the JWT
     *
     * @param request HTTP request
     * @return userId as Long
     * @throws IllegalArgumentException if header/token is invalid or claim missing/non-numeric
     */
    public Long extractUserIdFromRequest(HttpServletRequest request) {
        String token = extractBearerTokenFromRequest(request);
        return extractUserIdFromJwt(token);
    }

    // ======= Device  fingerprint methods

    /**
     * Extracts the device fingerprint claim from the raw JWT
     *
     * @param token raw JWT token
     * @return fingerprint trimmed and non-blank
     * @throws IllegalArgumentException if token invalid/expired or claim missing/blank
     */
    public String extractDeviceFingerprintFromJwt(String token) {
        validateTokenNotExpired(token);
        final String fp = jwtService.getDeviceFingerprint(token);
        if (fp == null || fp.isBlank()) {
            throw new IllegalArgumentException("Missing X-Device-Fingerprint header");
        } else { return fp; }
    }

    /**
     * Reads the device fingerprint from X-Device-Fingerprint header
     *
     * @param request HTTP request
     * @return fingerprint trimmed and non-blank
     * @throws IllegalArgumentException if the header is missing or blank
     */
    public String extractFingerprintFromHeader(HttpServletRequest request) {
        final String fp = request.getHeader(FINGERPRINT_HEADER);
        if (fp == null || fp.isBlank()) {
            throw new IllegalArgumentException("Missing X-Device-Fingerprint header");
        } else { return fp.trim(); }
    }


    // Combined fingerprint resolution =============================0

    /**
     * Resolves the device fingerprint using these steps:
     * 1. Try JWT claim if a Bearer token exists and is valid
     * 2. If not available, try X-Device-Fingerprint header
     *
     * Returns null if neither source provides a value or invalid
     *
     * @param request HTTP request
     * @return fingerprint trimmed, or null if not found
     */
    public String resolveDeviceFingerprint(HttpServletRequest request) {
        final String token = safeExtractBearerTokenOrNull(request);
        String fingerprint = null;

        if (token != null) { // try to get from JWT
            try { fingerprint = extractDeviceFingerprintFromJwt(token);
            } catch (Exception ignored) {} }

        if (fingerprint == null || fingerprint.isBlank()) {
            fingerprint = extractFingerprintFromHeader(request); } // try to get from header

        if (fingerprint == null || fingerprint.isBlank()) {
            return null;
        } else { return fingerprint.trim(); }
    }

    /**
     * Attempts to extract the bearer token from the request.
     * Returns null if the Authorization header is missing or invalid.
     *
     * @param request HTTP request
     * @return raw JWT token or null
     */
    private String safeExtractBearerTokenOrNull(HttpServletRequest request) {
        try {
            return extractBearerTokenFromRequest(request);
        } catch (Exception ignored) {
            return null;
        }
    }
}
