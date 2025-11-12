package cl.ufro.dci.naivepayapi.autentificacion.util;

/**
 * Utility class for device fingerprint sanitization.
 * Ensures fingerprints are never null and provides consistent handling across the application.
 */
public final class FingerprintSanitizer {

    // Private constructor to prevent instantiation
    private FingerprintSanitizer() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Sanitizes a device fingerprint by converting null to empty string.
     *
     * @param fingerprint Raw fingerprint that may be null
     * @return Empty string if fingerprint is null, otherwise the original fingerprint
     */
    public static String sanitize(String fingerprint) {
        return fingerprint != null ? fingerprint : "";
    }
}
