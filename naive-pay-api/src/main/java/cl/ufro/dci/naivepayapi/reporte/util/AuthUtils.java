package cl.ufro.dci.naivepayapi.reporte.util;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

/**
 * Utility class for authentication and user data extraction.
 *
 * <p>Provides helper methods to safely obtain the {@code userId}
 * from the Spring Security authentication context.</p>
 *
 * @since 1.0
 */
public class AuthUtils {

    /**
     * Retrieves the user ID from the authentication context.
     * <p>
     * Throws an HTTP 400 ({@link HttpStatus#BAD_REQUEST}) if the value
     * cannot be converted to {@link Long}.
     *
     * @param auth authentication context (must not be {@code null})
     * @return user ID as {@link Long}
     * @throws ResponseStatusException if the authentication context is invalid
     *                                 or the ID is not a valid number
     */
    public static Long getUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unable to retrieve userId from authentication context");
        }
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid userId: " + auth.getName());
        }
    }
}
