package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
import cl.ufro.dci.naivepayapi.autentificacion.dto.LoginRequest;
import cl.ufro.dci.naivepayapi.autentificacion.dto.LoginResponse;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import cl.ufro.dci.naivepayapi.dispositivos.service.DeviceService;
import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${naivepay.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${naivepay.security.lockout-window-minutes:30}")
    private int lockoutWindowMinutes;

    private final JWTService jwtService;
    private final AuthAttemptService authAttemptService;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;
    private final DeviceService deviceService;
    private final AccountLockService accountLockService;

    // =========================== LOGIN ===========================
    public ResponseEntity<?> login(LoginRequest req, String deviceFingerprint) {
        MDC.put("identifier", req.getIdentifier());
        MDC.put("deviceFingerprint", deviceFingerprint != null ? deviceFingerprint : "N/A");

        try {
            logger.debug("Intento de login recibido | identifier={}", req.getIdentifier());

            // Validación mínima de entrada
            if (isBlank(req.getIdentifier()) || isBlank(req.getPassword())) {
                logger.warn("Login rechazado: credenciales incompletas");
                return unauthorized(AuthAttemptReason.BAD_CREDENTIALS);
            }

            // 1) Resolver usuario por email o RUT
            Optional<User> userOpt = resolveUser(req.getIdentifier());
            if (userOpt.isEmpty()) {
                logger.warn("Login rechazado: usuario no encontrado | identifier={}", req.getIdentifier());
                // No podemos registrar el intento porque no hay user ni device
                return unauthorized(AuthAttemptReason.USER_NOT_FOUND);
            }
            User user = userOpt.get();

            MDC.put("userId", String.valueOf(user.getUseId()));
            MDC.put("email", user.getRegister().getRegEmail());

            // 2) Verificar que el email esté verificado
            if (!user.getRegister().isRegVerified()) {
                logger.warn("Login rechazado: email no verificado | userId={} | email={}",
                    user.getUseId(), user.getRegister().getRegEmail());
                logFailedAttempt(user, AuthAttemptReason.EMAIL_NOT_VERIFIED);
                return forbidden(AuthAttemptReason.EMAIL_NOT_VERIFIED);
            }

            // 3) Verificar si la cuenta está bloqueada
            if (accountLockService.isAccountLocked(user)) {
                logger.warn("Login rechazado: cuenta bloqueada | userId={} | email={}",
                    user.getUseId(), user.getRegister().getRegEmail());
                logFailedAttempt(user, AuthAttemptReason.ACCOUNT_BLOCKED);
                return forbidden(AuthAttemptReason.ACCOUNT_BLOCKED);
            }

            // 4) Verificar contraseña
            if (!isValidPassword(user, req.getPassword())) {
                logger.warn("Login rechazado: credenciales inválidas | userId={}", user.getUseId());
                logFailedAttempt(user, AuthAttemptReason.BAD_CREDENTIALS);

                // Verificar y bloquear cuenta si es necesario después de fallo
                boolean wasBlocked = accountLockService.checkAndBlockIfNeeded(user);

                // Si ya quedó bloqueada, responder 403 inmediatamente
                if (wasBlocked) {
                    logger.warn("Cuenta bloqueada automáticamente tras intento fallido | userId={}", user.getUseId());
                    return forbidden(AuthAttemptReason.ACCOUNT_BLOCKED);
                }

                // Calcular intentos restantes
                int remainingAttempts = calculateRemainingAttempts(user);
                logger.debug("Intentos restantes: {} | userId={}", remainingAttempts, user.getUseId());

                if (remainingAttempts == 0) {
                    logger.warn("Bloqueando cuenta: intentos agotados | userId={}", user.getUseId());
                    accountLockService.blockAccount(user);
                    logFailedAttempt(user, AuthAttemptReason.ACCOUNT_BLOCKED);
                    return forbidden(AuthAttemptReason.ACCOUNT_BLOCKED);
                }

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error", AuthAttemptReason.BAD_CREDENTIALS.name(),
                                "remainingAttempts", remainingAttempts
                        ));
            }

            // 5) Crear sesión autenticada con token y dispositivo autorizado
            try {
                LoginResponse response = createAuthenticatedSession(user, deviceFingerprint);
                logger.info("Login exitoso | userId={} | email={} | jti={}",
                    user.getUseId(), user.getRegister().getRegEmail(), response.getJti());
                return ResponseEntity.ok(response);
            } catch (ResponseStatusException ex) {
                logger.warn("Login rechazado: error de autorización de dispositivo | userId={} | reason={}",
                    user.getUseId(), ex.getReason());
                return handleDeviceAuthorizationError(user, ex);
            }
        } finally {
            MDC.remove("identifier");
            MDC.remove("deviceFingerprint");
            MDC.remove("userId");
            MDC.remove("email");
        }
    }

    // =========================== LOGOUT ===========================

    /**
     * Cierra la sesión actual del usuario invalidando el token JWT.
     *
     * @param authHeader Header Authorization con el token Bearer (ej: "Bearer eyJhbGci...")
     * @return ResponseEntity con mensaje de éxito (200) si se cierra correctamente,
     *         o error de autenticación (401) si el token es inválido o falta
     */
    public ResponseEntity<Map<String, Object>> logout(String authHeader) {
        try {
            logger.debug("Solicitud de logout recibida");

            String token = extractBearer(authHeader);
            if (token == null) {
                logger.warn("Logout rechazado: token ausente o inválido");
                return unauthorizedLogout();
            }

            try {
                UUID jti = UUID.fromString(jwtService.getJti(token));
                MDC.put("jti", jti.toString());

                authSessionService.closeByJti(jti);

                logger.info("Logout exitoso | jti={}", jti);
                return ResponseEntity.ok(Map.of("message", "Sesión cerrada", "jti", jti));
            } catch (JwtException | IllegalArgumentException ex) {
                logger.warn("Logout rechazado: token JWT inválido | error={}", ex.getMessage());
                return unauthorizedLogout();
            } finally {
                MDC.remove("jti");
            }
        } catch (Exception ex) {
            logger.error("Error inesperado durante logout | error={}", ex.getMessage(), ex);
            return unauthorizedLogout();
        }
    }

    // ----------------- Helpers - Login Flow -----------------

    /**
     * Crea una sesión autenticada completa: genera token JWT, valida dispositivo y persiste sesión.
     * Sigue la cadena: Session -> AuthAttempt -> Device -> User
     *
     * @param user Usuario autenticado
     * @param deviceFingerprint Fingerprint del dispositivo
     * @return LoginResponse con token, expiración y session ID
     * @throws ResponseStatusException si el dispositivo no está autorizado
     */
    private LoginResponse createAuthenticatedSession(User user, String deviceFingerprint) {
        logger.debug("Creando sesión autenticada | userId={}", user.getUseId());

        // Generar token JWT con JTI único
        UUID jti = UUID.randomUUID();
        String safeFingerprint = (deviceFingerprint == null) ? "" : deviceFingerprint;

        String token = jwtService.generate(
                String.valueOf(user.getUseId()),
                safeFingerprint,
                jti.toString()
        );
        Instant exp = jwtService.getExpiration(token);

        logger.debug("Token JWT generado | userId={} | jti={} | expiration={}", user.getUseId(), jti, exp);

        // Validar y obtener dispositivo autorizado
        Long userIdFromToken = Long.valueOf(jwtService.getUserId(token));
        Device device = deviceService.ensureAuthorizedDevice(userIdFromToken, safeFingerprint);

        logger.debug("Dispositivo autorizado | userId={} | fingerprint={}", user.getUseId(), device.getFingerprint());

        // 1. Crear AuthAttempt exitoso
        var initialAuthAttempt = authAttemptService.log(device, true, AuthAttemptReason.OK);

        logger.debug("AuthAttempt inicial creado | attemptId={}", initialAuthAttempt.getAttId());

        // 2. Crear Session con el AuthAttempt inicial
        Session session = authSessionService.saveActiveSession(jti, initialAuthAttempt, exp);

        logger.debug("Sesión persistida | userId={} | sessionId={}", user.getUseId(), session.getSesId());

        // Construir respuesta
        return new LoginResponse(
                token,
                exp.toString(),
                session.getSesId().toString()
        );
    }

    /**
     * Maneja errores de autorización de dispositivo durante el login.
     *
     * @param user Usuario que intenta autenticarse
     * @param ex Excepción lanzada por deviceService
     * @return ResponseEntity con código 403 y razón del error
     * @throws ResponseStatusException si el error no es de autorización de dispositivo
     */
    private ResponseEntity<?> handleDeviceAuthorizationError(User user, ResponseStatusException ex) {
        if (!ex.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
            throw ex;
        }

        String reason = ex.getReason();

        if (AuthAttemptReason.DEVICE_REQUIRED.name().equals(reason)) {
            logFailedAttempt(user, AuthAttemptReason.DEVICE_REQUIRED);
            return forbidden(AuthAttemptReason.DEVICE_REQUIRED);
        }

        if (AuthAttemptReason.DEVICE_UNAUTHORIZED.name().equals(reason)) {
            logFailedAttempt(user, AuthAttemptReason.DEVICE_UNAUTHORIZED);
            return forbidden(AuthAttemptReason.DEVICE_UNAUTHORIZED);
        }

        throw ex;
    }

    // ----------------- Helpers - User Resolution -----------------

    /** Resuelve usuario por email o por RUT con DV. */
    private Optional<User> resolveUser(String identifier) {
        final String id = identifier.trim();
        if (RutUtils.isEmail(id)) {
            return userRepo.findByRegisterRegEmail(id);
        }
        var rut = RutUtils.parseRut(id).orElse(null);
        if (rut == null) return Optional.empty();
        try {
            Long rutNum = Long.parseLong(rut.rut());
            return userRepo.findByUseRutGeneral(rutNum)
                    .filter(u -> Character.toUpperCase(u.getUseVerificationDigit()) == rut.dv());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }


    // ----------------- Helpers - Logging -----------------

    /**
     * Registra un intento fallido obteniendo automáticamente el dispositivo del usuario.
     * Sigue la cadena: Session -> AuthAttempt -> Device -> User
     */
    private void logFailedAttempt(User user, AuthAttemptReason reason) {
        deviceService.findByUserId(user.getUseId())
                .ifPresent(dev -> authAttemptService.log(dev, false, reason));
    }

    // ----------------- Helpers - Validation -----------------

    /** Valida que la contraseña proporcionada coincida con la almacenada en el registro del usuario. */
    private boolean isValidPassword(User user, String rawPassword) {
        var register = user.getRegister();

        if (register == null || isBlank(register.getRegHashedLoginPassword())) {
            return false;
        }

        return passwordEncoder.matches(rawPassword, register.getRegHashedLoginPassword());
    }

    // ----------------- Helpers - Utilities -----------------

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Extrae el token JWT de un header Authorization Bearer.
     *
     * @param authHeader Header Authorization completo (ej: "Bearer eyJhbGci...")
     * @return Token JWT sin el prefijo "Bearer ", o null si el header es inválido
     */
    private String extractBearer(String authHeader) {
        if (isBlank(authHeader) || !authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * Calcula los intentos de login restantes antes del bloqueo de cuenta.
     *
     * @param user Usuario para el cual calcular intentos restantes
     * @return Número de intentos restantes (0-maxFailedAttempts)
     */
    private int calculateRemainingAttempts(User user) {
        // El contador se reinicia con el último intento exitoso.
        // Se cuenta desde max(último_exitoso, ahora-lockoutWindowMinutes) para que
        // tras un login correcto vuelva al máximo en el siguiente fallo.
        Instant windowStart = Instant.now().minus(lockoutWindowMinutes, java.time.temporal.ChronoUnit.MINUTES);
        Instant lastSuccess = authAttemptService.findLastSuccessAt(user.getUseId());

        // Reiniciar desde el último éxito de login (o lockoutWindowMinutes atrás, lo que sea más reciente)
        Instant since = (lastSuccess != null && lastSuccess.isAfter(windowStart))
                ? lastSuccess
                : windowStart;

        long failedCount = authAttemptService.countFailedAttemptsSince(user.getUseId(), since);
        int remaining = Math.max(0, maxFailedAttempts - (int) failedCount);

        logger.debug("Intentos restantes calculados | userId={} | failedCount={} | remaining={}",
            user.getUseId(), failedCount, remaining);

        return remaining;
    }

    // ----------------- Helpers - Response Builders -----------------

    /** Construye respuesta 401 Unauthorized con el motivo del rechazo. */
    private static ResponseEntity<Map<String, Object>> unauthorized(AuthAttemptReason reason) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", reason.name()));
    }

    /** Construye respuesta 403 Forbidden con el motivo del rechazo. */
    private static ResponseEntity<Map<String, Object>> forbidden(AuthAttemptReason reason) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", reason.name()));
    }

    /** Construye respuesta 401 Unauthorized genérica para errores de logout. */
    private static ResponseEntity<Map<String, Object>> unauthorizedLogout() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "UNAUTHORIZED"));
    }
}