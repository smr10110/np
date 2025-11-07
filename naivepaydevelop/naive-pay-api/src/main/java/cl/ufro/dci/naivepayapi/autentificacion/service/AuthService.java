package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
import cl.ufro.dci.naivepayapi.autentificacion.dto.LoginRequest;
import cl.ufro.dci.naivepayapi.autentificacion.dto.LoginResponse;
import cl.ufro.dci.naivepayapi.autentificacion.service.impl.JWTServiceImpl;
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

    private final JWTServiceImpl jwtService;
    private final AuthAttemptService authAttemptService;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService authSessionService;
    private final DeviceService deviceService;

    public AuthService(
            JWTServiceImpl jwtService,
            AuthAttemptService authAttemptService,
            UserRepository userRepo,
            PasswordEncoder passwordEncoder,
            AuthSessionService authSessionService,
            DeviceService deviceService
    ) {
        this.jwtService = jwtService;
        this.authAttemptService = authAttemptService;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authSessionService = authSessionService;
        this.deviceService = deviceService;
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
            // Registrar intento con motivo USER_NOT_FOUND y el resto nulo (user, device, session)
            logAttempt(null, deviceFingerprint, null, false, AuthAttemptReason.USER_NOT_FOUND);
            return unauthorized(AuthAttemptReason.USER_NOT_FOUND);
        }
        User user = userOpt.get();

        // 2) Verificar contraseña desde REGISTER
        var register = user.getRegister();
        if (register == null
                || isBlank(register.getRegHashedLoginPassword())
                || !passwordEncoder.matches(req.getPassword(), register.getRegHashedLoginPassword())) {
            Device dev = deviceService.findByUserId(user.getUseId()).orElse(null);
            if (dev != null) {
                logAttempt(user, dev.getFingerprint(), null, false, AuthAttemptReason.BAD_CREDENTIALS);
            }
            return unauthorized(AuthAttemptReason.BAD_CREDENTIALS);
        }

        // 3) Generar token + persistir sesión
        UUID jti = UUID.randomUUID();
        if (deviceFingerprint == null) deviceFingerprint = "";
        String token = jwtService.generate(
                String.valueOf(user.getUseId()),
                deviceFingerprint,
                jti.toString()
        );
        Instant exp = jwtService.getExpiration(token);

        // Verifica que la fingerprint coincida con la vinculada al usuario
        Long userIdFromToken = Long.valueOf(jwtService.getUserId(token));
        Device device;
        try {
            device = deviceService.ensureAuthorizedDevice(userIdFromToken, deviceFingerprint);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
                String reason = ex.getReason();
                if (AuthAttemptReason.DEVICE_REQUIRED.name().equals(reason)) {
                    Device dev = deviceService.findByUserId(user.getUseId()).orElse(null);
                    if (dev != null) {
                        logAttempt(user, dev.getFingerprint(), null, false, AuthAttemptReason.DEVICE_REQUIRED);
                    }
                    return forbidden(AuthAttemptReason.DEVICE_REQUIRED);
                }
                if (AuthAttemptReason.DEVICE_UNAUTHORIZED.name().equals(reason)) {
                    Device dev = deviceService.findByUserId(user.getUseId()).orElse(null);
                    if (dev != null) {
                        logAttempt(user, dev.getFingerprint(), null, false, AuthAttemptReason.DEVICE_UNAUTHORIZED);
                    }
                    return forbidden(AuthAttemptReason.DEVICE_UNAUTHORIZED);
                }
            }
            throw ex;
        }

        Session auth = authSessionService.saveActiveSession(
                jti, user, device, exp
        );

        // Registrar éxito (OK)
        logAttempt(user, device.getFingerprint(), auth, true, AuthAttemptReason.OK);

        return ResponseEntity.ok(new LoginResponse(
                token,
                exp.toString(),
                auth.getSesId().toString()
        ));
    }

    // =========================== LOGOUT ===========================
    public ResponseEntity<Map<String, Object>> logout(String authHeader) {
        String token = extractBearer(authHeader);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "UNAUTHORIZED"));
        }
        try {
            UUID jti = UUID.fromString(jwtService.getJti(token));
            authSessionService.closeByJti(jti);

            return ResponseEntity.ok(Map.of("message", "Sesión cerrada", "jti", jti));
        } catch (JwtException | IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "UNAUTHORIZED"));
        }
    }

    // ----------------- Helpers -----------------

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

    /** Envuelve registro de intentos con reason estandarizado. */
    private void logAttempt(User user, String attDeviceFingerprint, Session session, boolean success, AuthAttemptReason reason) {
        authAttemptService.log(user, attDeviceFingerprint, session, success, reason);
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    /** Extrae token de un header Authorization Bearer. */
    private String extractBearer(String authHeader) {
        if (isBlank(authHeader) || !authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }

    /** 401 uniforme con payload {"error": "<reason>"} */
    private static ResponseEntity<Map<String, Object>> unauthorized(AuthAttemptReason reason) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", reason.name()));
    }

    /** 403 uniforme con payload {"error": "<reason>"} */
    private static ResponseEntity<Map<String, Object>> forbidden(AuthAttemptReason reason) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", reason.name()));
    }
}