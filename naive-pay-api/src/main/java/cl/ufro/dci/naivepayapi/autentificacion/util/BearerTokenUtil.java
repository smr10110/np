package cl.ufro.dci.naivepayapi.autentificacion.util;

/**
 * Utility class for Bearer token operations.
 * Provides methods to extract and validate Bearer tokens from Authorization headers.
 */
public final class BearerTokenUtil {

    private static final String BEARER_PREFIX = "Bearer ";

    // Private constructor to prevent instantiation
    private BearerTokenUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Extracts the JWT token from an Authorization Bearer header.
     *
     * @param authHeader Full Authorization header (e.g., "Bearer eyJhbGci...")
     * @return JWT token without the "Bearer " prefix, or null if the header is invalid
     */
    public static String extractToken(String authHeader) {
        if (isBlank(authHeader) || !authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * Checks if a string is blank (null or whitespace only).
     *
     * @param s String to check
     * @return true if the string is blank, false otherwise
     */
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
