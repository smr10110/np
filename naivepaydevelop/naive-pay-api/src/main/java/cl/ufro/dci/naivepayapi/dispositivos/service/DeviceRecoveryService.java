package cl.ufro.dci.naivepayapi.dispositivos.service;

import cl.ufro.dci.naivepayapi.autentificacion.dto.LoginResponse;
import cl.ufro.dci.naivepayapi.autentificacion.service.AuthSessionService;
import cl.ufro.dci.naivepayapi.autentificacion.service.impl.JWTServiceImpl;
import cl.ufro.dci.naivepayapi.dispositivos.domain.DeviceRecovery;
import cl.ufro.dci.naivepayapi.dispositivos.repository.DeviceRecoveryRepository;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.domain.Register;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import cl.ufro.dci.naivepayapi.registro.service.EmailService;
import cl.ufro.dci.naivepayapi.autentificacion.service.RutUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class DeviceRecoveryService {

    private static final long TTL_MINUTES = 10; // expiración del código

    private final DeviceRecoveryRepository repo;
    private final EmailService emailService;
    private final DeviceService deviceService;
    private final JWTServiceImpl jwtService;
    private final AuthSessionService authSessionService;
    private final UserRepository userRepo;

    /**
     * Inicia la recuperación: genera código, guarda el challenge y envía email.
     * @param identifier email o RUT con DV
     * @param fingerprint fingerprint del dispositivo actual
     * @param ip ip del cliente
     * @param ua user-agent
     * @return entidad persistida con estado PENDING
     */
    @Transactional
    public DeviceRecovery start(String identifier, String fingerprint, String ip, String ua) {
        User user = resolveUser(identifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        Register register = user.getRegister();

        String code = generateCode6();
        Instant now = Instant.now();

        DeviceRecovery rec = DeviceRecovery.builder()
                .id(UUID.randomUUID())
                .userId(user.getUseId())
                .fingerprint(nullSafe(fingerprint))
                .code(code)
                .status("PENDING")
                .requestedAt(now)
                .expiresAt(now.plus(TTL_MINUTES, ChronoUnit.MINUTES))
                .ip(ip)
                .userAgent(ua)
                .build();

        repo.save(rec);

        // Envío de correo usando tu servicio existente
        emailService.sendDeviceRecoveryEmail(register.getRegEmail(), code);

        return rec;
    }

    /**
     * Verifica el código, vincula el dispositivo y emite un JWT listo para usar.
     */
    @Transactional
    public LoginResponse verifyAndLink(String recoveryIdStr,
                                       String code,
                                       String currentFingerprint,
                                       String type,
                                       String os,
                                       String browser) {

        UUID recId = parseUuid(recoveryIdStr);
        DeviceRecovery rec = repo.findByIdAndStatus(recId, "PENDING")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "RECOVERY_INVALID"));

        if (Instant.now().isAfter(rec.getExpiresAt())) {
            rec.setStatus("EXPIRED");
            repo.save(rec);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RECOVERY_EXPIRED");
        }

        if (!rec.getCode().equals(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CODE_INVALID");
        }

        // Seguridad: exigir que el verify venga desde el mismo dispositivo del request inicial
        if (!nullSafe(rec.getFingerprint()).equals(nullSafe(currentFingerprint))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FINGERPRINT_MISMATCH");
        }

        // Vincular dispositivo al usuario
        // Cambio (autenticación): ahora necesitamos la entidad Device persistida
        // para enlazar la sesión (Session) con FK real a Device.
        var savedDevice = deviceService.registerForUser(
                rec.getUserId(),
                currentFingerprint,
                type,
                os,
                browser
        );

        // Marcar verificado
        rec.setStatus("VERIFIED");
        rec.setVerifiedAt(Instant.now());
        repo.save(rec);

        // Emitir token + persistir sesión
        UUID jti = UUID.randomUUID();
        String token = jwtService.generate(String.valueOf(rec.getUserId()), currentFingerprint, jti.toString());
        Instant exp = jwtService.getExpiration(token);

        // Cambio (autenticación): la API de sesiones ahora recibe entidades
        // User y Device (no tipos primitivos). Por eso cargamos el User y
        // pasamos el Device devuelto por registerForUser.
        User user = userRepo.findById(rec.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        authSessionService.saveActiveSession(jti, user, savedDevice, exp);

        return new LoginResponse(token, exp.toString(), jti.toString());
    }

    // ---------------- helpers ----------------

    private Optional<User> resolveUser(String identifier) {
        if (identifier == null) return Optional.empty();
        String id = identifier.trim();
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

    private static String generateCode6() {
        int n = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format("%06d", n);
    }

    private static UUID parseUuid(String s) {
        try { return UUID.fromString(s); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RECOVERY_INVALID"); }
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
}