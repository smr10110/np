package cl.ufro.dci.naivepayapi.autentificacion.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Excepción lanzada cuando falla la autenticación por credenciales incorrectas.
 *
 * <p>Esta excepción incluye información sobre los intentos restantes antes del bloqueo,
 * permitiendo al cliente informar al usuario cuántos intentos le quedan.
 */
@Getter
public class AuthenticationFailedException extends ResponseStatusException {

    private final Integer remainingAttempts;

    /**
     * Crea una nueva excepción de autenticación fallida.
     *
     * @param reason Razón del fallo (ej: "BAD_CREDENTIALS")
     * @param remainingAttempts Intentos restantes antes del bloqueo (puede ser null)
     */
    public AuthenticationFailedException(String reason, Integer remainingAttempts) {
        super(HttpStatus.UNAUTHORIZED, reason);
        this.remainingAttempts = remainingAttempts;
    }

}
