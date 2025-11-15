package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
import cl.ufro.dci.naivepayapi.autentificacion.dto.LoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validador centralizado para requests de login.
 * Encapsula todas las reglas de validación de credenciales.
 */
@Component
public class LoginRequestValidator {

    /**
     * Valida que el request de login contenga credenciales completas y válidas.
     *
     * @param req Request de login a validar
     * @throws ResponseStatusException si el request es inválido
     */
    public void validate(LoginRequest req) {
        validateNotNull(req);
        validateIdentifier(req.getIdentifier());
        validatePassword(req.getPassword());
    }

    /**
     * Valida que el request no sea null.
     *
     * @param req Request de login
     * @throws ResponseStatusException si el request es null
     */
    private void validateNotNull(LoginRequest req) {
        if (req == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "LOGIN_REQUEST_NULL"
            );
        }
    }

    /**
     * Valida que el identifier (email o RUT) no esté vacío.
     *
     * @param identifier Email o RUT del usuario
     * @throws ResponseStatusException si el identifier está vacío
     */
    private void validateIdentifier(String identifier) {
        if (isBlank(identifier)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                AuthAttemptReason.BAD_CREDENTIALS.name()
            );
        }
    }

    /**
     * Valida que la contraseña no esté vacía.
     *
     * @param password Contraseña del usuario
     * @throws ResponseStatusException si la contraseña está vacía
     */
    private void validatePassword(String password) {
        if (isBlank(password)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                AuthAttemptReason.BAD_CREDENTIALS.name()
            );
        }
    }

    /**
     * Verifica si un string es null o está vacío después de trim.
     *
     * @param s String a validar
     * @return true si el string es null o vacío
     */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
