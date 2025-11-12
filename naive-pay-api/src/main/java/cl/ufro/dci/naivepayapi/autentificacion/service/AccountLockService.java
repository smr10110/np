package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.AuthAttempt;
import cl.ufro.dci.naivepayapi.autentificacion.repository.AuthAttemptRepository;
import cl.ufro.dci.naivepayapi.registro.domain.AccountState;
import cl.ufro.dci.naivepayapi.registro.domain.Register;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import cl.ufro.dci.naivepayapi.registro.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de bloqueo automático de cuentas por intentos fallidos de autenticación.
 *
 * <p>Este servicio implementa una política de seguridad que bloquea cuentas cuando se detectan
 * múltiples intentos fallidos de autenticación dentro de una ventana temporal configurable.
 *
 * <h2>Política de Bloqueo</h2>
 * <ul>
 *   <li>Bloquea tras 5 intentos fallidos consecutivos </li>
 *   <li>Solo considera intentos dentro de una ventana temporal (30 min)</li>
 *   <li>Notifica al usuario por email cuando su cuenta es bloqueada</li>
 * </ul>
 */

@Service
@RequiredArgsConstructor
public class AccountLockService {

    private static final Logger logger = LoggerFactory.getLogger(AccountLockService.class);

    /**
     * Número máximo de intentos fallidos consecutivos antes de bloquear.
     */
    @Value("${naivepay.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    /**
     * Ventana temporal en minutos para considerar intentos fallidos.
     */

    @Value("${naivepay.security.lockout-window-minutes:30}")
    private int lockoutWindowMinutes;

    private final AuthAttemptRepository authAttemptRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Verifica si una cuenta está bloqueada.
     *
     * <p>Una cuenta se considera bloqueada cuando su estado es {@link AccountState#INACTIVE}.
     *
     * @param user el usuario a verificar
     * @return {@code true} si la cuenta está bloqueada, {@code false} en caso contrario
     * @throws IllegalArgumentException si {@code user} es {@code null}
     */
    public boolean isAccountLocked(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        return user.getUseState() == AccountState.INACTIVE;
    }

    /**
     * Calculates the number of login attempts remaining before account lockout.
     * The counter resets after a successful login or after the lockout window expires.
     *
     * @param user User for whom to calculate remaining attempts
     * @return Number of attempts remaining (0 to maxFailedAttempts)
     */
    public int calculateRemainingAttempts(User user) {
        // Calculate window start: either last successful login or lockoutWindowMinutes ago
        Instant windowStart = Instant.now().minus(lockoutWindowMinutes, ChronoUnit.MINUTES);
        Instant lastSuccess = authAttemptRepository.findLastSuccessAt(user.getUseId());

        // Reset counter from last successful login (or lockout window, whichever is more recent)
        Instant since = (lastSuccess != null && lastSuccess.isAfter(windowStart))
                ? lastSuccess
                : windowStart;

        long failedCount = authAttemptRepository.countFailedAttemptsSince(user.getUseId(), since);
        int remaining = Math.max(0, maxFailedAttempts - (int) failedCount);

        logger.debug("Remaining attempts calculated | userId={} | failedCount={} | remaining={}",
                user.getUseId(), failedCount, remaining);

        return remaining;
    }

    /**
     * Verifica intentos fallidos recientes y bloquea la cuenta si es necesario.
     *
     * <p>Este metodo implementa la política de bloqueo automático siguiendo estos pasos:
     * <ol>
     *   <li>Valida que el usuario exista y no esté ya bloqueado</li>
     *   <li>Obtiene los últimos N intentos de autenticación del usuario</li>
     *   <li>Verifica que todos sean fallidos (sin ningún éxito intercalado)</li>
     *   <li>Verifica que ocurrieron dentro de la ventana temporal configurada</li>
     *   <li>Si ambas condiciones se cumplen, bloquea la cuenta</li>
     * </ol>
     *
     * @param user el usuario a verificar y potencialmente bloquear
     * @return {@code true} si la cuenta fue bloqueada como resultado de esta verificación,
     *         {@code false} si no fue necesario bloquear
     */
    @Transactional
    public boolean checkAndBlockIfNeeded(User user) {
        // Validación inicial: usuario válido
        if (user == null) {
            logger.debug("checkAndBlockIfNeeded llamado con usuario nulo");
            return false;
        }

        //skip si ya está bloqueado
        if (isAccountLocked(user)) {
            logger.debug("Usuario {} ya está bloqueado, omitiendo verificación", user.getUseId());
            return false;
        }

        // Obtener últimos N intentos
        List<AuthAttempt> recentAttempts = authAttemptRepository.findLatestAttemptsByUser(
                user.getUseId(),
                PageRequest.of(0, maxFailedAttempts)
        );

        // Condición 1: Debe tener al menos N intentos
        if (recentAttempts.size() < maxFailedAttempts) {
            logger.debug("Usuario {} tiene solo {} intentos, necesita {} para bloquear",
                    user.getUseId(), recentAttempts.size(), maxFailedAttempts);
            return false;
        }

        // Condición 2: Todos deben ser fallidos (sin éxitos intercalados)
        if (!areAllAttemptsFailed(recentAttempts)) {
            logger.debug("Usuario {} tiene intentos exitosos entre los recientes, no se bloquea", user.getUseId());
            return false;
        }

        // Condición 3: Deben estar dentro de la ventana temporal
        if (!areAttemptsWithinLockoutWindow(recentAttempts)) {
            logger.debug("Los intentos fallidos del usuario {} están fuera de la ventana temporal, no se bloquea", user.getUseId());
            return false;
        }

        // Todas las condiciones cumplidas → bloquear cuenta
        logger.info("Bloqueando cuenta del usuario {} debido a {} intentos fallidos en {} minutos",
                user.getUseId(), maxFailedAttempts, lockoutWindowMinutes);

        return blockAccount(user);
    }

    /**
     * Bloquea una cuenta estableciendo su estado a INACTIVE.
     *
     * <p>Este metodo realiza tres operaciones en secuencia:
     * <ol>
     *   <li><b>Bloqueo en BD:</b> Cambia estado y persiste</li>
     *   <li><b>Notificación email:</b> Envía aviso al usuario</li>
     * </ol>
     *
     * <p><b>Transaccional:</b> Si falla el guardado en BD, toda la transacción hace rollback.
     * El email y el log se ejecutan solo si el guardado fue exitoso.
     *
     * @param user el usuario a bloquear
     * @return {@code true} si el bloqueo fue exitoso
     * @throws IllegalArgumentException si {@code user} es {@code null}
     * @throws IllegalStateException si {@code user.getUseId()} es {@code null}
     * @throws org.springframework.dao.DataAccessException si falla el guardado en BD (propagada)
     */
    @Transactional
    public boolean blockAccount(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getUseId() == null) {
            throw new IllegalStateException("User must have an ID to be locked");
        }

        // Bloquea al usuario
        user.setUseState(AccountState.INACTIVE);
        userRepository.save(user);

        // Notifica via email
        sendBlockNotificationEmail(user);

        //log para auditoria
        logAccountBlocked(user);

        return true;
    }

    /**
     * Verifica si todos los intentos son fallidos.
     * Un intento es exitoso si {@link AuthAttempt#isAttSuccess()} retorna {@code true}.
     *
     * @param attempts lista de intentos a verificar
     * @return {@code true} si ningún intento fue exitoso
     */
    private boolean areAllAttemptsFailed(List<AuthAttempt> attempts) {
        // noneMatch retorna true si NINGUNO es exitoso (todos fallidos)
        return attempts.stream().noneMatch(AuthAttempt::isAttSuccess);
    }

    /**
     * Verifica si los intentos están dentro de la ventana temporal de bloqueo.
     *
     * <p>La ventana se calcula desde el momento actual hacia atrás. Solo se considera
     * el intento más antiguo de la lista (que viene ordenada del más reciente al más antiguo).
     *
     * @param attempts lista de intentos ordenada del más reciente al más antiguo
     * @return {@code true} si el intento más antiguo está dentro de la ventana
     */
    private boolean areAttemptsWithinLockoutWindow(List<AuthAttempt> attempts) {
        // El último elemento de la lista es el intento más antiguo
        Instant oldestAttemptTime = attempts.getLast().getAttOccurred();

        // Calcular límite de la ventana: ahora - N minutos
        Instant lockoutThreshold = Instant.now().minus(lockoutWindowMinutes, ChronoUnit.MINUTES);

        // El intento más antiguo debe ser DESPUÉS del límite (dentro de ventana)
        return oldestAttemptTime.isAfter(lockoutThreshold);
    }

    /**
     * Obtiene el email del usuario de forma segura.
     *
     * @param user el usuario del cual obtener el email
     * @return {@link Optional} con el email si existe y es válido, {@link Optional#empty()} si no
     */
    private Optional<String> getUserEmail(User user) {
        return Optional.ofNullable(user.getRegister())
                .map(Register::getRegEmail)
                .filter(email -> !email.isBlank());
    }

    /**
     * Envía notificación de bloqueo por email al usuario.
     * @param user el usuario al que enviar la notificación
     */
    private void sendBlockNotificationEmail(User user) {
        try {
            // Obtener email de y enviar notificación
            getUserEmail(user).ifPresentOrElse(
                    email -> {
                        emailService.sendAccountBlockedNotice(email);
                        logger.debug("Email de notificación de bloqueo enviado al usuario {}", user.getUseId());
                    },
                    () -> logger.debug("No se envió email al usuario {}: no tiene dirección de email válida", user.getUseId())
            );
        } catch (Exception mailEx) {
            logger.warn("No se pudo enviar email de notificación de bloqueo al usuario {}: {}",
                    user.getUseId(), mailEx.getMessage());
        }
    }

    /**
     * Registra el evento de bloqueo con structured logging.
     *
     * @param user el usuario cuya cuenta fue bloqueada
     */
    private void logAccountBlocked(User user) {
        // Agregar contexto al MDC para correlación en logs distribuidos
        MDC.put("userId", String.valueOf(user.getUseId()));
        MDC.put("rut", user.getUseRutGeneral() + "-" + user.getUseVerificationDigit());

        try {
            // Log con formato estructurado: pipe-separated para parsing
            logger.warn("Cuenta bloqueada por política de seguridad | " +
                            "userId={} | " +
                            "rut={}-{} | " +
                            "intentosFallidos={} | " +
                            "ventanaBloqueo={}min | " +
                            "timestamp={}",
                    user.getUseId(),
                    user.getUseRutGeneral(),
                    user.getUseVerificationDigit(),
                    maxFailedAttempts,
                    lockoutWindowMinutes,
                    Instant.now()
            );
        } finally {
            MDC.remove("userId");
            MDC.remove("rut");
        }
    }
}