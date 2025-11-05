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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Servicio para gestión de recuperación de contraseñas.
 * Maneja el flujo completo: generación de código, verificación y reseteo.
 */
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordRecoveryService.class);
    private static final int CODE_EXPIRATION_MINUTES = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordRecoveryRepository passwordRecoveryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Genera y envía un código de recuperación al email del usuario.
     * Invalida códigos PENDING anteriores antes de crear uno nuevo.
     *
     * @param email Email del usuario que solicita recuperación
     */
    @Transactional
    public void sendRecoveryCode(String email) {
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            logger.debug("Intento de recuperación para email no registrado");
            return;
        }

        User user = userOpt.get();

        // Invalidar códigos PENDING anteriores
        passwordRecoveryRepository.findLatestByUserIdAndStatus(user.getId(), PasswordRecoveryStatus.PENDING)
                .ifPresent(oldRecovery -> {
                    oldRecovery.setPasStatus(PasswordRecoveryStatus.EXPIRED);
                    passwordRecoveryRepository.save(oldRecovery);
                });

        String code = generateCode();
        Instant now = Instant.now();
        Instant expiration = now.plus(CODE_EXPIRATION_MINUTES, ChronoUnit.MINUTES);

        PasswordRecovery recovery = PasswordRecovery.builder()
                .user(user)
                .pasCode(code)
                .pasCreated(now)
                .pasExpired(expiration)
                .pasLastSent(now)
                .pasResendCount(0)
                .pasStatus(PasswordRecoveryStatus.PENDING)
                .build();

        passwordRecoveryRepository.save(recovery);
        emailService.sendPasswordRecoveryEmail(email, code);
        logger.info("Código de recuperación enviado exitosamente");
    }

    /**
     * Verifica que el código de recuperación sea válido.
     *
     * @param email Email del usuario
     * @param code Código de 6 dígitos enviado por email
     * @throws ResponseStatusException Si el código es inválido, expirado o ya usado
     */
    @Transactional
    public void verifyCode(String email, String code) {
        // Valida email, código, estado y expiración
        validateRecoveryCode(email, code);
    }

    /**
     * Resetea la contraseña del usuario usando el código de recuperación.
     * Desbloquea la cuenta si estaba INACTIVE y envía email de confirmación.
     *
     * @param email Email del usuario
     * @param code Código de recuperación válido
     * @param newPassword Nueva contraseña (se guardará hasheada)
     * @throws ResponseStatusException Si el código es inválido
     */
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        // Valida el código y obtiene el registro de recuperación
        PasswordRecovery recovery = validateRecoveryCode(email, code);
        User user = recovery.getUser();

        // Hashea y guarda la nueva contraseña
        user.getRegister().setHashedLoginPassword(passwordEncoder.encode(newPassword));

        // Marca el código como usado
        recovery.setPasStatus(PasswordRecoveryStatus.USED);
        recovery.setPasUsed(Instant.now());

        // Desbloquea la cuenta si estaba bloqueada
        if (user.getState() == AccountState.INACTIVE) {
            user.setState(AccountState.ACTIVE);
            logger.info("Cuenta desbloqueada tras proceso de recuperación");
        }

        // Envía email de confirmación al usuario (notifica cambio exitoso)
        emailService.sendPasswordChangeConfirmation(email, user.getNames());

        logger.info("Contraseña actualizada exitosamente mediante recuperación");
    }

    /**
     * Valida que el código de recuperación sea correcto y esté vigente.
     * Verifica: existencia del usuario, código válido, estado PENDING y no expiración.
     *
     * @param email Email del usuario
     * @param code Código de recuperación
     * @return PasswordRecovery válido
     * @throws ResponseStatusException Si alguna validación falla
     */
    private PasswordRecovery validateRecoveryCode(String email, String code) {
        // Busca el usuario
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CODE"));

        // Busca el código de recuperación
        PasswordRecovery recovery = passwordRecoveryRepository.findByUser_IdAndPasCode(user.getId(), code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CODE"));

        // Verifica que el código no haya sido usado
        if (recovery.getPasStatus() != PasswordRecoveryStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CODE_ALREADY_USED");
        }

        // Verifica que el código no haya expirado (10 minutos)
        if (recovery.getPasExpired().isBefore(Instant.now())) {
            recovery.setPasStatus(PasswordRecoveryStatus.EXPIRED);
            passwordRecoveryRepository.save(recovery);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CODE_EXPIRED");
        }

        return recovery;
    }

    /**
     * Genera un código numérico aleatorio de 6 dígitos.
     *
     * @return Código de recuperación (000000-999999)
     */
    private String generateCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1000000));
    }
}