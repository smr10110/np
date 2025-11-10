package cl.ufro.dci.naivepayapi.reporte.util;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

/**
 * Utilidades para autenticación y obtención de datos del usuario.
 */
public class AuthUtils {
    /**
     * Obtiene el userId del contexto de autenticación.
     * Lanza una excepción HTTP 400 si el valor no es convertible a Long.
     *
     * @param auth contexto de autenticación
     * @return userId como Long
     */
    public static Long getUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo obtener el userId del contexto de autenticación");
        }
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El userId no es válido: " + auth.getName());
        }
    }
}

