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
public class AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JWTService jwtService;
    private final AuthAttemptService authAttemptService;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;
    private final DeviceService deviceService;
    private final AccountLockService accountLockService;

    public AuthService(
            JWTService jwtService,
            AuthAttemptService authAttemptService,
            UserRepository userRepo,
            PasswordEncoder passwordEncoder,
            AuthSessionService authSessionService,
            DeviceService deviceService,
            AccountLockService accountLockService
    ) {
        this.jwtService = jwtService;
        this.authAttemptService = authAttemptService;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authSessionService = authSessionService;
        this.deviceService = deviceService;
        this.accountLockService = accountLockService;
    }

    // =========================== LOGIN ===========================
    public ResponseEntity<?> login(LoginRequest req, String deviceFingerprint) {
        // Validación mínima de entrada
        if (isBlank(req.getIdentifier()) || isBlank(req.getPassword())) {
            return unauthorized(AuthAttemptReason.BAD_CREDENTIALS);
        }

        // 1) Resolver usuario por email o RUT
        Optional<User> userOpt = resolveUser(req.getIdentifier());
        if (userOpt.isEmpty()) {
            logAttempt(null, deviceFingerprint, null, false, AuthAttemptReason.USER_NOT_FOUND);
            return unauthorized(AuthAttemptReason.USER_NOT_FOUND);
        }
        User user = userOpt.get();

        // 2) Verificar si la cuenta está bloqueada
        if (accountLockService.isAccountLocked(user)) {
            logFailedAttempt(user, AuthAttemptReason.ACCOUNT_BLOCKED);
            return forbidden(AuthAttemptReason.ACCOUNT_BLOCKED);
        }

        // 3) Verificar contraseña
        if (!isValidPassword(user, req.getPassword())) {
            logFailedAttempt(user, AuthAttemptReason.BAD_CREDENTIALS);

            // Verificar y bloquear cuenta si es necesario después de fallo
            boolean wasBlocked = accountLockService.checkAndBlockIfNeeded(user);

            // Si ya quedó bloqueada, responder 403 inmediatamente
            if (wasBlocked) {
                return forbidden(AuthAttemptReason.ACCOUNT_BLOCKED);
            }

            // Calcular intentos restantes
            int remainingAttempts = calculateRemainingAttempts(user);

            if (remainingAttempts == 0) {
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

        // 4) Crear sesión autenticada con token y dispositivo autorizado
        try {
            LoginResponse response = createAuthenticatedSession(user, deviceFingerprint);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException ex) {
            return handleDeviceAuthorizationError(user, ex);
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
        String token = extractBearer(authHeader);
        if (token == null) {
            return unauthorizedLogout();
        }
        try {
            UUID jti = UUID.fromString(jwtService.getJti(token));
            authSessionService.closeByJti(jti);

            return ResponseEntity.ok(Map.of("message", "Sesión cerrada", "jti", jti));
        } catch (JwtException | IllegalArgumentException ex) {
            return unauthorizedLogout();
        }
    }

    // ----------------- Helpers - Login Flow -----------------

    /**
     * Crea una sesión autenticada completa: genera token JWT, valida dispositivo y persiste sesión.
     *
     * @param user Usuario autenticado
     * @param deviceFingerprint Fingerprint del dispositivo
     * @return LoginResponse con token, expiración y session ID
     * @throws ResponseStatusException si el dispositivo no está autorizado
     */
    private LoginResponse createAuthenticatedSession(User user, String deviceFingerprint) {
        // Generar token JWT con JTI único
        UUID jti = UUID.randomUUID();
        String safeFingerprint = (deviceFingerprint == null) ? "" : deviceFingerprint;

        String token = jwtService.generate(
                String.valueOf(user.getId()),
                safeFingerprint,
                jti.toString()
        );
        Instant exp = jwtService.getExpiration(token);

        // Validar y obtener dispositivo autorizado
        Long userIdFromToken = Long.valueOf(jwtService.getUserId(token));
        Device device = deviceService.ensureAuthorizedDevice(userIdFromToken, safeFingerprint);

        // Persistir sesión activa
        Session session = authSessionService.saveActiveSession(jti, user, device, exp);

        // Registrar intento exitoso
        logAttempt(user, device.getFingerprint(), session, true, AuthAttemptReason.OK);

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
            return userRepo.findByEmail(id);
        }
        var rut = RutUtils.parseRut(id).orElse(null);
        if (rut == null) return Optional.empty();
        try {
            Long rutNum = Long.parseLong(rut.rut());
            return userRepo.findByRutGeneral(rutNum)
                    .filter(u -> Character.toUpperCase(u.getVerificationDigit()) == rut.dv());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }


    // ----------------- Helpers - Logging -----------------

    /** Registra un intento de autenticación con toda la información disponible. */
    private void logAttempt(User user, String attDeviceFingerprint, Session session, boolean success, AuthAttemptReason reason) {
        authAttemptService.log(user, attDeviceFingerprint, session, success, reason);
    }

    /** Registra un intento fallido obteniendo automáticamente el dispositivo del usuario. */
    private void logFailedAttempt(User user, AuthAttemptReason reason) {
        deviceService.findByUserId(user.getId())
                .ifPresent(dev -> authAttemptService.log(user, dev.getFingerprint(), null, false, reason));
    }

    // ----------------- Helpers - Validation -----------------

    /** Valida que la contraseña proporcionada coincida con la almacenada en el registro del usuario. */
    private boolean isValidPassword(User user, String rawPassword) {
        var register = user.getRegister();

        if (register == null || isBlank(register.getHashedLoginPassword())) {
            return false;
        }

        return passwordEncoder.matches(rawPassword, register.getHashedLoginPassword());
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
     * @return Número de intentos restantes (0-5)
     */
    private int calculateRemainingAttempts(User user) {
        // Cambio: el contador ahora se reinicia con el ÃƒÅ¡LTIMO Ãƒâ€°XITO.
        // Se cuenta desde max(ÃƒÂºltimo_exitoso, ahora-30min) para que
        // tras un login correcto vuelva a 5 en el siguiente fallo.
        Instant thirtyMinutesAgo = Instant.now().minus(30, java.time.temporal.ChronoUnit.MINUTES);
        Instant lastSuccess = authAttemptService.findLastSuccessAt(user.getId());
        // Reiniciar desde el ÃƒÂºltimo ÃƒÂ©xito de login (o 30min atrÃƒÂ¡s, lo que sea mÃƒÂ¡s reciente)
        Instant since = (lastSuccess != null && lastSuccess.isAfter(thirtyMinutesAgo))
                ? lastSuccess
                : thirtyMinutesAgo;
        long failedCount = authAttemptService.countFailedAttemptsSince(user.getId(), since);
        return Math.max(0, 5 - (int) failedCount);
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