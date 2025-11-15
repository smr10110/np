package cl.ufro.dci.naivepayapi.autentificacion.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Excepción personalizada para errores de autenticación que pueden incluir
 * información sobre intentos de login restantes.
 */
@Getter
public class AuthenticationFailedException extends ResponseStatusException {
    private final Integer remainingAttempts;

    public AuthenticationFailedException(HttpStatus status, String errorCode) {
        this(status, errorCode, null);
    }

    public AuthenticationFailedException(HttpStatus status, String errorCode, Integer remainingAttempts) {
        super(status, errorCode);
        this.remainingAttempts = remainingAttempts;
    }
}
