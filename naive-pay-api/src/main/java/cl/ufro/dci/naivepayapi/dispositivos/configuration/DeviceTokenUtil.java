package cl.ufro.dci.naivepayapi.dispositivos.configuration;

import cl.ufro.dci.naivepayapi.autentificacion.service.JWTService;
import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import cl.ufro.dci.naivepayapi.dispositivos.repository.DeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 Utilities for working with JWT and device fingerprint.
 Extracts and validates the token.
 Retrieves claims (userId, jti, fingerprint).
 Validates that the request comes from the linked device.
 */
@Component
public class DeviceTokenUtil {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String FP_HEADER = "X-Device-Fingerprint";

    private final JWTService jwt;
    private final DeviceRepository deviceRepo;

    public DeviceTokenUtil(JWTService jwt, DeviceRepository deviceRepo) {
        this.jwt = jwt;
        this.deviceRepo = deviceRepo;
    }

    /**
     * Extracts the raw token from an Authorization header value.
     *
     * @param authorizationHeader value of the Authorization header (must be "Bearer <token>")
     * @return the token without the "Bearer " prefix
     * @throws IllegalArgumentException if the header is missing or does not follow the expected format
     */
    public String extractRawToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("Falta header Authorization");
        }
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Formato de Authorization inválido (esperado: Bearer <token>)");
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    /**
     * Extracts the raw token by reading the Authorization header from the request.
     *
     * @param request HttpServletRequest containing the Authorization header
     * @return the token without the "Bearer " prefix
     * @throws IllegalArgumentException if the header is missing or invalid
     * @see #extractRawToken(String)
     */
    public String extractRawToken(HttpServletRequest request) {
        return extractRawToken(request.getHeader(AUTH_HEADER));
    }

    /**
     * Validates a token:
     * - It is parseable.
     * - It is not expired.
     *
     * @param rawToken token without the "Bearer " prefix
     * @throws IllegalArgumentException if the token is invalid or expired
     */
    public void validate(String rawToken) {
        try {
            jwt.parse(rawToken);
            if (jwt.isExpired(rawToken)) {
                throw new IllegalArgumentException("Token expirado");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Token inválido");
        }
    }

    /**
     * Retrieves the {@code userId} claim from an Authorization header.
     *
     * @param authorizationHeader Authorization header
     * @return userId as a Long
     * @throws IllegalArgumentException if the token is missing, invalid, or does not contain {@code userId}
     */
    public Long getUserId(String authorizationHeader) {
        String token = extractRawToken(authorizationHeader);
        validate(token);
        String userId = jwt.getUserId(token);
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("El token no contiene el claim 'userId'");
        }
        return Long.valueOf(userId);
    }

    /**
     * Retrieves the {@code userId} claim by reading the Authorization header from the request.
     *
     * @param request HttpServletRequest containing the Authorization header
     * @return userId as a Long
     * @throws IllegalArgumentException if the token is missing, invalid, or does not contain {@code userId}
     * @see #getUserId(String)
     */
    public Long getUserId(HttpServletRequest request) {
        return getUserId(request.getHeader(AUTH_HEADER));
    }


    /**
     * Retrieves the device fingerprint claim from an Authorization header.
     * May return null if the claim is not included in the JWT.
     *
     * @param authorizationHeader Authorization header
     * @return the JWT fingerprint or {@code null} if it does not exist
     * @throws IllegalArgumentException if the token is missing or invalid
     */
    public String getDeviceFingerprintFromToken(String authorizationHeader) {
        String token = extractRawToken(authorizationHeader);
        validate(token);
        return jwt.getDeviceFingerprint(token); // puede ser null si aún no lo agregas al JWT
    }

    /**
     * Retrieves the device fingerprint claim by reading the Authorization header from the request.
     *
     * @param request HttpServletRequest containing the Authorization header
     * @return the JWT fingerprint or {@code null} if it does not exist
     * @throws IllegalArgumentException if the token is missing or invalid
     * @see #getDeviceFingerprintFromToken(String)
     */

    public String getDeviceFingerprintFromToken(HttpServletRequest request) {
        return getDeviceFingerprintFromToken(request.getHeader(AUTH_HEADER));
    }

    /**
     * Retrieves the device fingerprint directly from the HTTP {@code X-Device-Fingerprint} header.
     *
     * @param request HttpServletRequest containing the corresponding header
     * @return the fingerprint read from the header, or {@code null} if it does not exist
     */
    public String getDeviceFingerprintFromHeader(HttpServletRequest request) {
        String fp = request.getHeader(FP_HEADER);
        return (fp == null || fp.isBlank()) ? null : fp;
    }

    /**
     * Verifies that the request originates from the device linked to the authenticated user.
     * - Automatically retrieves the {@code userId} from the JWT.
     * - Checks that the fingerprint from the JWT or header matches the stored device fingerprint.
     *
     * @param request HttpServletRequest containing Authorization and X-Device-Fingerprint headers
     * @return the {@link Device} linked to the user if verification succeeds
     * @throws IllegalArgumentException if the token or fingerprint are invalid, or the device does not match
     */
    public Device requireLinkedDevice(HttpServletRequest request) {
        Long userId = getUserId(request);
        String fp = null;
        try {
            fp = getDeviceFingerprintFromToken(request);
        } catch (Exception ignored) { }
        if (fp == null || fp.isBlank()) {
            fp = getDeviceFingerprintFromHeader(request);
        }
        if (fp == null || fp.isBlank()) {
            throw new IllegalArgumentException("Missing device fingerprint (JWT or X-Device-Fingerprint header)");
        }
        Device device = deviceRepo.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("No device currently linked"));

        if (device.getFingerprint() == null || !fp.equals(device.getFingerprint())) {
            throw new IllegalArgumentException("This device is not authorized for the account");
        }
        return device;
    }


    // LA FUNCION ------------------------------------------------------------------------------------------------------------------


    /**
     * Resolves the request's fingerprint, prioritizing the one obtained from the JWT
     * and using the {@code X-Device-Fingerprint} header as a fallback.
     *
     * @param request   HttpServletRequest containing authentication headers
     * @param rawToken  JWT token without the "Bearer " prefix
     * @return the resolved fingerprint, or {@code null} if it could not be obtained
     */
    private String resolveFingerprint(HttpServletRequest request, String rawToken) {
        String fp = null;
        try { fp = jwt.getDeviceFingerprint(rawToken); } catch (Exception ignored) {}
        if (fp == null || fp.isBlank()) fp = getDeviceFingerprintFromHeader(request);
        return (fp == null || fp.isBlank()) ? null : fp;
    }

    /**
     * Checks whether the request originates from the device linked to the authenticated user.
     * Automatically retrieves the userId from the JWT.
     *
     * @param request HttpServletRequest containing Authorization and X-Device-Fingerprint headers
     * @return true if the device matches the one linked to the user, false otherwise
     */
    public boolean isCurrentRequestAuthorized(HttpServletRequest request) {
        try {
            final String raw = extractRawToken(request);
            validate(raw);

            final String userIdStr = jwt.getUserId(raw);
            if (userIdStr == null || userIdStr.isBlank()) return false;
            final Long userId = Long.valueOf(userIdStr);

            final String fp = resolveFingerprint(request, raw);
            if (fp == null) return false;

            return deviceRepo.findByUser_Id(userId)
                    .map(d -> fp.equals(d.getFingerprint()))
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }
}
