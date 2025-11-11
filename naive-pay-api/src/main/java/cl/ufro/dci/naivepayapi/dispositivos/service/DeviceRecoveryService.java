package cl.ufro.dci.naivepayapi.dispositivos.service;

import cl.ufro.dci.naivepayapi.autentificacion.dto.LoginResponse;
import cl.ufro.dci.naivepayapi.autentificacion.service.AuthSessionService;
import cl.ufro.dci.naivepayapi.autentificacion.service.AuthAttemptService;
import cl.ufro.dci.naivepayapi.autentificacion.service.RutUtils;
import cl.ufro.dci.naivepayapi.autentificacion.service.impl.JWTServiceImpl;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import cl.ufro.dci.naivepayapi.dispositivos.domain.DeviceRecovery;
import cl.ufro.dci.naivepayapi.dispositivos.repository.DeviceRecoveryRepository;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.domain.Register;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import cl.ufro.dci.naivepayapi.registro.service.EmailService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;


/**
 * Service responsible for handling device recovery operations
 * <p>
 * Allows users to request a recovery code using either their email or RUT, verify it, and link a new device by issuing a valid JWT session token
 *<p>
 * Typical flow:
 * <p>
 * 1. User requests a recovery code via {@link #requestRecovery(String, String)}.
 * <p>
 * 2. User verifies the code via {@link #verifyAndLink(String, String, String, String, String, String)}.
 */
@Service
@RequiredArgsConstructor
public class DeviceRecoveryService {
    private final DeviceRecoveryRepository deviceRecoveryRepo;
    private final EmailService emailService;
    private final DeviceService deviceService;
    private final JWTServiceImpl jwtService;
    private final AuthSessionService authSessionService;
    private final AuthAttemptService authAttemptService;
    private final UserRepository userRepo;

    private enum RecoveryStatus {
        Pending, Verified, Expired
    }
    private static final long expirationTime_Min = 10;

    /**
     * Starts a device recovery process for a user identified by email or RUT.
     *
     * @param identifier  user identifier (email or RUT)
     * @param reqFingerprint device fingerprint from the requesting device
     *
     * @return the created {@link DeviceRecovery} entity
     * @throws ResponseStatusException if no matching user is found
     */
    @Transactional
    public DeviceRecovery requestRecovery(String identifier,
                                          String reqFingerprint) {
        User user = resolveIdentifier(identifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        Register register = user.getRegister();
        DeviceRecovery recovery = createRecovery(user, reqFingerprint);

        deviceRecoveryRepo.save(recovery);
        emailService.sendDeviceRecoveryEmail(register.getRegEmail(), recovery.getCode());

        return recovery;
    }

    /**
     * Generates a random six-character alphanumeric recovery code.
     *
     * @return recovery code string
     */
    private String generateRecoveryCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /**
     * Creates a {@link DeviceRecovery} instance for the given user and fingerprint
     *
     * @param user          the associated user
     * @param fingerprint   device fingerprint
     * @return a new {@link DeviceRecovery} instance
     */
    private DeviceRecovery createRecovery(User user,
                                          String fingerprint) {
        Instant now = Instant.now();

        DeviceRecovery recovery = new DeviceRecovery();
        recovery.setId(UUID.randomUUID());
        recovery.setUser(user);
        recovery.setFingerprint(fingerprint);
        recovery.setCode(generateRecoveryCode());
        recovery.setStatus(RecoveryStatus.Pending.name());
        recovery.setRequestedAt(now);
        recovery.setExpiresAt(now.plus(expirationTime_Min, ChronoUnit.MINUTES));

        return recovery;
    }

    /**
     * Verifies a recovery code and links the device to the user account.
     * Generates a JWT token and stores an active session for the verified device.
     *
     * @param recoveryId      recovery request id
     * @param code            recovery code sent to the user via email
     * @param reqFingerprint  device fingerprint used for verification
     * @param type            device type
     * @param os              os name
     * @param browser         browser
     * @return {@link LoginResponse} containing the generated token and expiration
     * @throws ResponseStatusException if recovery is invalid, expired, or mismatched
     */
    @Transactional
    public LoginResponse verifyAndLink(String recoveryId,
                                       String code,
                                       String reqFingerprint,
                                       String type,
                                       String os,
                                       String browser) {

        DeviceRecovery recovery = getPendingRecovery(recoveryId);
        validateNotExpired(recovery);
        validateCode(recovery, code);
        validateFingerprint(recovery, reqFingerprint);

        User user = recovery.getUser();
        Long userId = user.getUseId();
        Device savedDevice = deviceService.registerForUser(userId, reqFingerprint, type, os, browser);

        markRecoveryVerified(recovery);

        return issueTokenAndCreateSession(user, savedDevice, reqFingerprint);
    }

    private static UUID transformUUID(String s) {
        try { return UUID.fromString(s);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RECOVERY_INVALID"); } }

    /**
     * Gets a pending device recovery by its id and status.
     *
     * @param devRecoveryId recovery ID string
     * @return the {@link DeviceRecovery} entity if found
     * @throws ResponseStatusException if not found or invalid
     */
    private DeviceRecovery getPendingRecovery(String devRecoveryId) {
        UUID recId = transformUUID(devRecoveryId);
        DeviceRecovery recovery = deviceRecoveryRepo.findByIdAndStatus(recId, RecoveryStatus.Pending.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "RECOVERY_INVALID"));
        return recovery;
    }

    /**
     * Validates that the recovery request is not expired by time
     * Marks it as expired and throws an exception if validation fails
     *
     * @param recovery the recovery entity
     *
     * @throws ResponseStatusException if the recovery has expired
     */
    private void validateNotExpired(DeviceRecovery recovery) {
        if (Instant.now().isAfter(recovery.getExpiresAt())) {
            recovery.setStatus(RecoveryStatus.Expired.name());
            deviceRecoveryRepo.save(recovery);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RECOVERY_EXPIRED");
        }
    }

    /**
     * Validates that the provided code matches the one stored in the recovery record.
     *
     * @param recovery the recovery entity
     * @param code     the provided code
     *
     * @throws ResponseStatusException if the code is invalid
     */
    private void validateCode(DeviceRecovery recovery, String code) {
        if (!recovery.getCode().equals(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CODE_INVALID");
        }
    }

    /**
     * Validates that the current device fingerprint matches the one from the recovery request.
     *
     * @param recovery           the recovery entity
     * @param currentFingerprint the fingerprint provided during verification
     *
     * @throws ResponseStatusException if the fingerprints do not match
     */
    private void validateFingerprint(DeviceRecovery recovery, String currentFingerprint) {
        String expectedFingerprint = recovery.getFingerprint();
        String providedFingerprint = currentFingerprint;

        if (expectedFingerprint == null) {
            expectedFingerprint = "";
        }
        if (providedFingerprint == null) {
            providedFingerprint = "";
        }

        boolean fingerprintsMatch = expectedFingerprint.equals(providedFingerprint);
        if (!fingerprintsMatch) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FINGERPRINT_MISMATCH");}
    }

    /**
     * Marks the recovery record as verified and updates its timestamp.
     *
     * @param recovery the verified recovery entity
     */
    private void markRecoveryVerified(DeviceRecovery recovery) {
        recovery.setStatus(RecoveryStatus.Verified.name());
        recovery.setVerifiedAt(Instant.now());
        deviceRecoveryRepo.save(recovery);
    }

    /**
     * Generates a JWT token for the verified user and device, and stores an active session.
     * Follows the chain: Session -> AuthAttempt -> Device -> User
     *
     * @param user        the verified user
     * @param savedDevice the verified device
     * @param fingerprint the device fingerprint
     * @return a {@link LoginResponse} containing token, expiration, and JTI
     */
    private LoginResponse issueTokenAndCreateSession(User user,
                                                     Device savedDevice,
                                                     String fingerprint) {

        UUID jti = UUID.randomUUID();
        String token = jwtService.generate(String.valueOf(user.getUseId()), fingerprint, jti.toString());
        Instant exp = jwtService.getExpiration(token);

        // 1. Crear AuthAttempt exitoso para device recovery
        var initialAuthAttempt = authAttemptService.log(savedDevice, true, AuthAttemptReason.OK);

        // 2. Crear Session con el AuthAttempt
        var session = authSessionService.saveActiveSession(jti, initialAuthAttempt, exp);

        return new LoginResponse(token, exp.toString(), session.getSesId().toString());
    }

    /**
     * Resolves a user by email or RUT.
     *
     * @param identifier user identifier (email or RUT)
     * @return {@link Optional} containing the user if found, otherwise empty
     */
    private Optional<User> resolveIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String trimmedId = identifier.trim();

        if (RutUtils.isEmail(trimmedId)) {
            return findByEmail(trimmedId);
        }
        return findByRut(trimmedId);
    }

    /**
     * Finds a user by email address.
     *
     * @param email user email
     * @return {@link Optional} containing the user if found
     */
    private Optional<User> findByEmail(String email) {
        return userRepo.findByRegisterRegEmail(email);
    }

    /**
     * Finds a user by RUT and validates its verification digit (DV) if it matches
     *
     * @param rawRut raw RUT string, with or without separators
     *
     * @return {@link Optional} containing the user if RUT and DV match
     */
    private Optional<User> findByRut(String rawRut) {
        var rutParsed = RutUtils.parseRut(rawRut).orElse(null);
        if (rutParsed == null) {return Optional.empty();}
        try {
            Long rutNumber = Long.parseLong(rutParsed.rut());
            char dv = rutParsed.dv(); // "digito-verificador"
            return userRepo.findByUseRutGeneral(rutNumber)
                    .filter(user -> Character.toUpperCase(user.getUseVerificationDigit()) == dv);
        } catch (NumberFormatException e) {
            return Optional.empty();}
    }

}