package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
import cl.ufro.dci.naivepayapi.autentificacion.dto.LoginRequest;
import cl.ufro.dci.naivepayapi.autentificacion.dto.LoginResponse;
import cl.ufro.dci.naivepayapi.autentificacion.exception.AuthenticationFailedException;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import cl.ufro.dci.naivepayapi.dispositivos.service.DeviceService;
import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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

    private final JWTService jwtService;
    private final AuthAttemptService authAttemptService;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;
    private final DeviceService deviceService;
    private final AccountLockService accountLockService;
    private final LoginRequestValidator loginRequestValidator;

    // =========================== LOGIN ===========================
    public ResponseEntity<?> login(LoginRequest req, String deviceFingerprint) {
        setupLoginMDC(req.getIdentifier(), deviceFingerprint);
        User user = null;

        try {
            logger.debug("Intento de login recibido | identifier={}", req.getIdentifier());

            // 1) Validar request
            loginRequestValidator.validate(req);

            // 2) Encontrar usuario
            user = findUserByIdentifier(req.getIdentifier());
            addUserToMDC(user);

            // 3) Verificar que cuenta no esté bloqueada
            validateAccountNotLocked(user);

            // 4) Validar contraseña
            validatePassword(user, req.getPassword());

            // 5) Crear sesión autenticada
            LoginResponse response = createAuthenticatedSession(user, deviceFingerprint);
            logger.info("Login exitoso | userId={} | email={} | jti={}",
                user.getUseId(), user.getRegister().getRegEmail(), response.getJti());
            return ResponseEntity.ok(response);

        } catch (AuthenticationFailedException ex) {
            return handleAuthenticationFailure(ex);
        } catch (ResponseStatusException ex) {
            return handleResponseStatusException(ex, user);
        } finally {
            clearLoginMDC();
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
        logger.debug("Solicitud de logout recibida");
        String token = extractBearer(authHeader);

        try {
            UUID jti = UUID.fromString(jwtService.getJti(token));
            MDC.put("jti", jti.toString());

            authSessionService.closeByJti(jti);

            logger.info("Logout exitoso | jti={}", jti);
            return ResponseEntity.ok(Map.of("message", "Sesión cerrada", "jti", jti));
        } catch (JwtException | IllegalArgumentException ex) {
            logger.warn("Logout rechazado: token JWT inválido | error={}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "UNAUTHORIZED"));
        } finally {
            MDC.remove("jti");
        }
    }

    // ----------------- Validation Methods -----------------

    /**
     * Busca un usuario por su identificador (email o RUT).
     *
     * @param identifier Email o RUT del usuario
     * @return Usuario encontrado
     * @throws ResponseStatusException si el usuario no existe
     */
    User findUserByIdentifier(String identifier) {
        return resolveUser(identifier)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                AuthAttemptReason.USER_NOT_FOUND.name()
            ));
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

    /**
     * Valida que la cuenta del usuario no esté bloqueada.
     *
     * @param user Usuario a validar
     * @throws ResponseStatusException si la cuenta está bloqueada
     */
    void validateAccountNotLocked(User user) {
        if (accountLockService.isAccountLocked(user)) {
            logger.warn("Login rechazado: cuenta bloqueada | userId={} | email={}",
                user.getUseId(), user.getRegister().getRegEmail());
            accountLockService.logFailedAttempt(user, AuthAttemptReason.ACCOUNT_BLOCKED);
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                AuthAttemptReason.ACCOUNT_BLOCKED.name()
            );
        }
    }

    /** Valida que la contraseña proporcionada coincida con la almacenada en el registro del usuario. */
    private boolean isValidPassword(User user, String rawPassword) {
        var register = user.getRegister();

        if (register == null || isBlank(register.getRegHashedLoginPassword())) {
            return false;
        }

        return passwordEncoder.matches(rawPassword, register.getRegHashedLoginPassword());
    }

    /**
     * Valida la contraseña del usuario y maneja el fallo si es incorrecta.
     *
     * @param user Usuario a autenticar
     * @param password Contraseña en texto plano
     * @throws ResponseStatusException si la contraseña es incorrecta
     */
    void validatePassword(User user, String password) {
        if (!isValidPassword(user, password)) {
            handlePasswordFailure(user);
        }
    }

    /**
     * Maneja el fallo de validación de contraseña registrando intentos y verificando bloqueos.
     *
     * @param user Usuario que falló la autenticación
     * @throws ResponseStatusException con información de intentos restantes o bloqueo
     */
    private void handlePasswordFailure(User user) {
        logger.warn("Login rechazado: credenciales inválidas | userId={}", user.getUseId());

        // Si no tiene device, no podemos registrar el intento ni mostrar contador
        if (deviceService.findByUserId(user.getUseId()).isEmpty()) {
            logger.debug("Usuario sin device, retornando error genérico | userId={}", user.getUseId());
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                AuthAttemptReason.BAD_CREDENTIALS.name()
            );
        }

        // Registrar intento fallido y calcular intentos restantes
        int remainingAttempts = accountLockService.handleFailedAuthentication(user);

        // Si agotó los intentos, bloquear ANTES de lanzar excepción
        if (remainingAttempts <= 0) {
            logger.warn("Bloqueando cuenta por agotamiento de intentos | userId={}", user.getUseId());
            accountLockService.blockAccount(user);
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                AuthAttemptReason.ACCOUNT_BLOCKED.name()
            );
        }

        // Lanzar excepción con información de intentos restantes
        throw new AuthenticationFailedException(
            AuthAttemptReason.BAD_CREDENTIALS.name(),
            remainingAttempts
        );
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
     * Maneja excepciones de autenticación fallida con información de intentos restantes.
     *
     * @param ex Excepción de autenticación fallida
     * @return ResponseEntity con código 401 y detalles del error
     */
    private ResponseEntity<?> handleAuthenticationFailure(AuthenticationFailedException ex) {
        logger.warn("Login rechazado: credenciales inválidas | remainingAttempts={}", ex.getRemainingAttempts());
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of(
                        "error", ex.getReason(),
                        "remainingAttempts", ex.getRemainingAttempts()
                ));
    }

    /**
     * Maneja excepciones generales de validación y errores de dispositivo.
     *
     * @param ex Excepción lanzada
     * @param user Usuario que intenta autenticarse (puede ser null si falla antes de encontrarlo)
     * @return ResponseEntity con código de error apropiado
     */
    private ResponseEntity<?> handleResponseStatusException(ResponseStatusException ex, User user) {
        String reason = ex.getReason();

        // Manejo específico para DEVICE_UNAUTHORIZED (necesita registrar intento)
        if (user != null && AuthAttemptReason.DEVICE_UNAUTHORIZED.name().equals(reason)) {
            accountLockService.logFailedAttempt(user, AuthAttemptReason.DEVICE_UNAUTHORIZED);
        }

        // Manejo genérico: retorna error con status code apropiado
        logger.warn("Login rechazado | status={} | reason={}", ex.getStatusCode(), reason);
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", reason));
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

    // ----------------- Helpers - MDC Management -----------------

    /**
     * Configura el contexto MDC con información inicial del request de login.
     *
     * @param identifier Email o RUT del usuario
     * @param deviceFingerprint Fingerprint del dispositivo
     */
    private void setupLoginMDC(String identifier, String deviceFingerprint) {
        MDC.put("identifier", identifier);
        MDC.put("deviceFingerprint", deviceFingerprint != null ? deviceFingerprint : "N/A");
    }

    /**
     * Agrega información del usuario autenticado al contexto MDC.
     *
     * @param user Usuario autenticado
     */
    private void addUserToMDC(User user) {
        MDC.put("userId", String.valueOf(user.getUseId()));
        MDC.put("email", user.getRegister().getRegEmail());
    }

    /**
     * Limpia todo el contexto MDC relacionado con el login.
     */
    private void clearLoginMDC() {
        MDC.remove("identifier");
        MDC.remove("deviceFingerprint");
        MDC.remove("userId");
        MDC.remove("email");
    }
}