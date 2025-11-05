package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.AuthAttempt;
import cl.ufro.dci.naivepayapi.autentificacion.repository.AuthAttemptRepository;
import cl.ufro.dci.naivepayapi.registro.domain.AccountState;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import cl.ufro.dci.naivepayapi.registro.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Servicio encargado de gestionar el bloqueo automático de cuentas
 * tras múltiples intentos fallidos de autenticación.
 * - Bloquea cuenta tras 5 intentos fallidos consecutivos
 * - Considera solo intentos dentro de una ventana de 30 minutos
 */
@Service
public class AccountLockService {

    private static final Logger logger = LoggerFactory.getLogger(AccountLockService.class);

    /**
     * Número máximo de intentos fallidos antes de bloquear la cuenta.
     */
    private static final int MAX_FAILED_ATTEMPTS = 5;

    /**
     * Ventana de tiempo en minutos para contar los intentos fallidos.
     * Solo se bloquea si los intentos ocurren dentro de este período.
     */
    private static final int LOCKOUT_DURATION_MINUTES = 30;

    private final AuthAttemptRepository authAttemptRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public AccountLockService(
            AuthAttemptRepository authAttemptRepository,
            UserRepository userRepository, EmailService emailService) {
        this.authAttemptRepository = authAttemptRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Verifica si una cuenta está bloqueada.
     *
     * @param user Usuario a verificar
     * @return true si la cuenta está en estado INACTIVE (bloqueada), false en caso contrario
     */
    public boolean isAccountLocked(User user) {
        return user.getState() == AccountState.INACTIVE;
    }

    /**
     * Verifica los intentos fallidos recientes y bloquea la cuenta si es necesario.
     *
     * <p>Lógica de bloqueo:
     * <ol>
     *   <li>Obtiene los últimos 5 intentos de autenticación del usuario</li>
     *   <li>Verifica si todos son fallidos</li>
     *   <li>Verifica si ocurrieron dentro de los últimos 30 minutos</li>
     *   <li>Si ambas condiciones se cumplen, bloquea la cuenta</li>
     * </ol>
     *
     * @param user Usuario a verificar y potencialmente bloquear
     * @return true si la cuenta fue bloqueada, false si no fue necesario bloquear
     */
    @Transactional
    public boolean checkAndBlockIfNeeded(User user) {
        if (user == null) {
            return false;
        }

        if (isAccountLocked(user)) {
            return false;
        }

        List<AuthAttempt> recentAttempts = authAttemptRepository.findLatestAttemptsByUser(
                user.getId(),
                PageRequest.of(0, MAX_FAILED_ATTEMPTS)
        );

        if (recentAttempts.size() < MAX_FAILED_ATTEMPTS) {
            return false;
        }

        boolean allFailed = recentAttempts.stream()
                .noneMatch(AuthAttempt::isAttSuccess);

        if (!allFailed) {
            return false;
        }

        Instant oldestAttemptTime = recentAttempts.getLast().getAttOccurred();
        Instant lockoutThreshold = Instant.now().minus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES);

        if (oldestAttemptTime.isBefore(lockoutThreshold)) {
            return false;
        }

        return blockAccount(user);
    }

    /**
     * Bloquea una cuenta de usuario estableciendo su estado a INACTIVE.
     *
     * @param user Usuario a bloquear
     * @return true si el bloqueo fue exitoso
     */
    @Transactional
    public boolean blockAccount(User user) {
        try {
            user.setState(AccountState.INACTIVE);
            userRepository.save(user);

            // Enviar correo informativo sobre el bloqueo y pasos de recuperacion
            try {
                var register = user.getRegister();
                String email = (register != null) ? register.getEmail() : null;
                if (email != null && !email.isBlank()) {
                    emailService.sendAccountBlockedNotice(email);
                } else {
                    logger.debug("No se envia correo de bloqueo: usuario {} sin email registrado", user.getId());
                }
            } catch (Exception mailEx) {
                logger.warn("No se pudo enviar correo de cuenta bloqueada al usuario {}: {}", user.getId(), mailEx.getMessage());
            }

            logger.warn("Cuenta bloqueada por seguridad. Usuario: {}, RUT: {}-{}, intentos: {}, ventana_min: {}, ts: {}",
                    user.getId(), user.getRutGeneral(), user.getVerificationDigit(),
                    MAX_FAILED_ATTEMPTS, LOCKOUT_DURATION_MINUTES, Instant.now());
            return true;

        } catch (Exception e) {
            logger.error("Error al bloquear cuenta del usuario {}: {}", user.getId(), e.getMessage(), e);
            return false;
        }
    }
}