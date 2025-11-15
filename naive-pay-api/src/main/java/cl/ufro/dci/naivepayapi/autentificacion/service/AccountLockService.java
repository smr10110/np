package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.dispositivos.service.DeviceService;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final DeviceService deviceService;
    private final AuthAttemptService authAttemptService;

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
        return user.getUseState() == AccountState.INACTIVE;
    }

    /**
     * Bloquea una cuenta estableciendo su estado a INACTIVE.
     * @param user el usuario a bloquear
     * @return {@code true} si el bloqueo fue exitoso
     * @throws IllegalArgumentException si {@code user} es {@code null}
     * @throws IllegalStateException si {@code user.getUseId()} es {@code null}
     * @throws org.springframework.dao.DataAccessException si falla el guardado en BD (propagada)
     */
    @Transactional
    public boolean blockAccount(User user) {

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
     * Calcula los intentos de login restantes antes del bloqueo de cuenta.
     *
     * @param user Usuario para el cual calcular intentos restantes
     * @return Número de intentos restantes (0-maxFailedAttempts)
     */
    public int calculateRemainingAttempts(User user) {

        // Calcular ventana temporal: ahora - N minutos
        Instant windowStart = Instant.now().minus(lockoutWindowMinutes, ChronoUnit.MINUTES);

        // Obtener último intento exitoso del usuario
        Instant lastSuccess = authAttemptRepository.findLastSuccessAt(user.getUseId());

        // Reiniciar desde el último éxito de login (o lockoutWindowMinutes atrás, lo que sea más reciente)
        Instant since = (lastSuccess != null && lastSuccess.isAfter(windowStart))
                ? lastSuccess
                : windowStart;

        // Contar intentos fallidos desde la fecha calculada
        long failedCount = authAttemptRepository.countFailedAttemptsSince(user.getUseId(), since);

        // Calcular intentos restantes
        int remaining = Math.max(0, maxFailedAttempts - (int) failedCount);

        logger.debug("Intentos restantes calculados | userId={} | failedCount={} | remaining={}",
                user.getUseId(), failedCount, remaining);

        return remaining;
    }

    /**
     * Maneja un fallo de autenticación registrando el intento y calculando intentos restantes.
     *
     * @param user Usuario que falló la autenticación
     * @return Número de intentos restantes (0 o negativo si debe bloquearse)
     */
    @Transactional
    public int handleFailedAuthentication(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        logger.debug("Manejando fallo de autenticación | userId={}", user.getUseId());

        // 1. Calcular intentos restantes ANTES de registrar el intento actual
        int remainingAttempts = calculateRemainingAttempts(user) - 1; // Restar 1 por el intento actual

        // 2. Registrar el intento fallido actual
        logFailedAttempt(user, AuthAttemptReason.BAD_CREDENTIALS);

        // 3. Retornar intentos restantes (puede ser 0 o negativo)
        logger.debug("Intentos restantes después del fallo | userId={} | remaining={}",
            user.getUseId(), remainingAttempts);

        return remainingAttempts;
    }

    /**
     * Registra un intento fallido obteniendo automáticamente el dispositivo del usuario.
     *
     * <p>Este método busca el dispositivo asociado al usuario y registra el intento
     * fallido con la razón especificada. Si el usuario no tiene dispositivo asociado,
     * no se registra ningún intento.
     *
     * @param user Usuario que falló la autenticación
     * @param reason Razón del fallo (ej: BAD_CREDENTIALS, ACCOUNT_BLOCKED, DEVICE_UNAUTHORIZED)
     */
    public void logFailedAttempt(User user, AuthAttemptReason reason) {
        deviceService.findByUserId(user.getUseId())
                .ifPresent(dev -> authAttemptService.log(dev, false, reason));
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
                        logger.info("Email de notificación de bloqueo enviado | userId={} | email={}",
                            user.getUseId(), email);
                    },
                    () -> logger.warn("No se envió email al usuario {}: no tiene dirección de email válida",
                        user.getUseId())
            );
        } catch (Exception mailEx) {
            logger.error("No se pudo enviar email de notificación de bloqueo | userId={} | error={}",
                user.getUseId(), mailEx.getMessage(), mailEx);
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