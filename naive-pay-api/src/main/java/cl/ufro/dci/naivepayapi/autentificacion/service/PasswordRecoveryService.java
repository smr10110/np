package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.PasswordRecovery;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.PasswordRecoveryStatus;
import cl.ufro.dci.naivepayapi.autentificacion.repository.PasswordRecoveryRepository;
import cl.ufro.dci.naivepayapi.registro.domain.AccountState;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.service.EmailService;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Servicio de recuperación de contraseñas mediante código de verificación.
 * Flujo: 1) Envío de código por email, 2) Validación + reseteo de contraseña.
 */
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordRecoveryService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${naivepay.security.recovery-code-expiration-minutes:10}")
    private int codeExpirationMinutes;

    private final PasswordRecoveryRepository passwordRecoveryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public void sendRecoveryCode(String email) {
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            logger.debug("Intento de recuperación para email no registrado");
            return;
        }

        User user = userOpt.get();

        //  Invalidar códigos anteriores y crear nuevo código
        invalidatePendingCodes(user.getId());
        PasswordRecovery recovery = createNewRecovery(user);
        passwordRecoveryRepository.save(recovery);

        // Envío de email
        sendRecoveryEmail(email, recovery.getPasCode());

        logRecoveryCodeSent(user);
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_SHORT");
        }

        // Validar código y actualizar contraseña en BD
        PasswordRecovery recovery = validateRecoveryCode(email, code);
        User user = recovery.getUser();

        user.getRegister().setHashedLoginPassword(passwordEncoder.encode(newPassword));
        recovery.setPasStatus(PasswordRecoveryStatus.USED);
        recovery.setPasUsed(Instant.now());

        // Desbloquea cuenta si estaba bloqueada
        if (user.getState() == AccountState.INACTIVE) {
            user.setState(AccountState.ACTIVE);
        }

        // Email de confirmación
        sendPasswordChangeEmail(email, user.getNames());

        logPasswordReset(user);
    }

    // === MÉTODOS HELPER: Validación ===

    /**
     * Valida código de recuperación verificando usuario, código, estado y expiración.
     *
     * @param email Email del usuario
     * @param code Código de 6 dígitos
     * @return PasswordRecovery válido
     * @throws ResponseStatusException Si el código es inválido, usado o expirado
     */
    private PasswordRecovery validateRecoveryCode(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CODE"));

        PasswordRecovery recovery = passwordRecoveryRepository.findByUser_IdAndPasCode(user.getId(), code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CODE"));

        if (recovery.getPasStatus() != PasswordRecoveryStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CODE_ALREADY_USED");
        }

        if (recovery.getPasExpired().isBefore(Instant.now())) {
            recovery.setPasStatus(PasswordRecoveryStatus.EXPIRED);
            passwordRecoveryRepository.save(recovery);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CODE_EXPIRED");
        }

        return recovery;
    }

    // === MÉTODOS HELPER: Lógica de negocio ===

    /**
     * Invalida códigos PENDING anteriores del usuario para evitar uso de códigos viejos.
     *
     * @param userId ID del usuario
     */
    private void invalidatePendingCodes(Long userId) {
        passwordRecoveryRepository.findLatestByUserIdAndStatus(userId, PasswordRecoveryStatus.PENDING)
                .ifPresent(oldRecovery -> {
                    oldRecovery.setPasStatus(PasswordRecoveryStatus.EXPIRED);
                    passwordRecoveryRepository.save(oldRecovery);
                });
    }

    /**
     * Crea nuevo registro de recuperación con código generado y fecha de expiración.
     *
     * @param user Usuario solicitante
     * @return Nuevo PasswordRecovery con estado PENDING
     */
    private PasswordRecovery createNewRecovery(User user) {
        String code = generateCode();
        Instant now = Instant.now();
        Instant expiration = now.plus(codeExpirationMinutes, ChronoUnit.MINUTES);

        return PasswordRecovery.builder()
                .user(user)
                .pasCode(code)
                .pasCreated(now)
                .pasExpired(expiration)
                .pasLastSent(now)
                .pasResendCount(0)
                .pasStatus(PasswordRecoveryStatus.PENDING)
                .build();
    }

    /**
     * Genera código numérico aleatorio de 6 dígitos (000000-999999).
     *
     * @return Código de verificación
     */
    private String generateCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1000000));
    }

    // === MÉTODOS HELPER: Operaciones secundarias (email) ===

    /**
     * Envía email con código de recuperación
     *
     * @param email Destinatario
     * @param code Código de verificación
     */
    private void sendRecoveryEmail(String email, String code) {
        try {
            emailService.sendPasswordRecoveryEmail(email, code);
            logger.debug("Email con código de recuperación enviado exitosamente");
        } catch (Exception mailEx) {
            logger.warn("Error al enviar email de recuperación a {}: {}", email, mailEx.getMessage());
        }
    }

    /**
     * Envía email de confirmación de cambio de contraseña, aislando errores.
     *
     * @param email Destinatario
     * @param userName Nombre del usuario
     */
    private void sendPasswordChangeEmail(String email, String userName) {
        try {
            emailService.sendPasswordChangeConfirmation(email, userName);
            logger.debug("Email de confirmación de cambio de contraseña enviado");
        } catch (Exception mailEx) {
            logger.warn("Error al enviar email de confirmación a {}: {}", email, mailEx.getMessage());
        }
    }

    // === MÉTODOS HELPER: Observabilidad (logging con MDC) ===

    /**
     * Registra envío de código de recuperación con contexto distribuido (MDC).
     *
     * @param user Usuario que solicitó recuperación
     */
    private void logRecoveryCodeSent(User user) {
        MDC.put("userId", String.valueOf(user.getId()));
        MDC.put("email", user.getRegister().getEmail());
        try {
            logger.info("Código de recuperación enviado | userId={} | email={}",
                user.getId(), user.getRegister().getEmail());
        } finally {
            MDC.remove("userId");
            MDC.remove("email");
        }
    }

    /**
     * Registra reseteo exitoso de contraseña.
     *
     * @param user Usuario que reseteó contraseña
     */
    private void logPasswordReset(User user) {
        MDC.put("userId", String.valueOf(user.getId()));
        MDC.put("email", user.getRegister().getEmail());
        MDC.put("accountUnblocked", String.valueOf(user.getState() == AccountState.ACTIVE));
        try {
            logger.info("Contraseña actualizada exitosamente | userId={} | email={} | cuentaDesbloqueada={}",
                user.getId(), user.getRegister().getEmail(), user.getState() == AccountState.ACTIVE);
        } finally {
            MDC.remove("userId");
            MDC.remove("email");
            MDC.remove("accountUnblocked");
        }
    }
}