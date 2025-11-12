package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service responsible for validating login preconditions and credentials.
 * Centralizes all validation logic for the authentication flow.
 */
@Service
@RequiredArgsConstructor
public class LoginValidator {

    private static final Logger logger = LoggerFactory.getLogger(LoginValidator.class);

    private final PasswordEncoder passwordEncoder;
    private final AccountLockService accountLockService;

    /**
     * Validates that credentials are not blank (null or whitespace only).
     *
     * @param identifier User identifier (email or RUT)
     * @param password Raw password
     * @throws ResponseStatusException if credentials are blank
     */
    public void validateCredentialsNotBlank(String identifier, String password) {
        if (isBlank(identifier) || isBlank(password)) {
            logger.warn("Login rejected: incomplete credentials");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AuthAttemptReason.BAD_CREDENTIALS.name());
        }
    }

    /**
     * Validates that the user's email is verified.
     *
     * @param user User to validate
     * @throws ResponseStatusException if email is not verified
     */
    public void validateEmailVerified(User user) {
        if (!user.getRegister().isRegVerified()) {
            logger.warn("Login rejected: email not verified | userId={} | email={}",
                    user.getUseId(), user.getRegister().getRegEmail());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, AuthAttemptReason.EMAIL_NOT_VERIFIED.name());
        }
    }

    /**
     * Validates that the user's account is not locked.
     *
     * @param user User to validate
     * @throws ResponseStatusException if account is locked
     */
    public void validateAccountNotLocked(User user) {
        if (accountLockService.isAccountLocked(user)) {
            logger.warn("Login rejected: account locked | userId={} | email={}",
                    user.getUseId(), user.getRegister().getRegEmail());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, AuthAttemptReason.ACCOUNT_BLOCKED.name());
        }
    }

    /**
     * Validates that the provided password matches the user's stored hashed password.
     *
     * @param user User whose password to validate
     * @param rawPassword Raw password to validate
     * @return true if password is valid, false otherwise
     */
    public boolean isPasswordValid(User user, String rawPassword) {
        var register = user.getRegister();

        if (register == null || isBlank(register.getRegHashedLoginPassword())) {
            return false;
        }

        return passwordEncoder.matches(rawPassword, register.getRegHashedLoginPassword());
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
