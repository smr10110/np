package cl.ufro.dci.naivepayapi.dispositivos.service;

import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import cl.ufro.dci.naivepayapi.dispositivos.domain.DeviceLog;
import cl.ufro.dci.naivepayapi.dispositivos.repository.DeviceLogRepository;
import cl.ufro.dci.naivepayapi.dispositivos.repository.DeviceRepository;
import cl.ufro.dci.naivepayapi.autentificacion.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository repo;
    private final DeviceLogRepository logRepo;
    private final UserRepository userRepo;
    private final SessionRepository sessionRepository;

    // add service from register to hash de fingerprint
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers or REPLACES the user's device (1 user - 1 device).
     * Now the PK is the fingerprint (String). The FK column "id" (unique) enforces 1:1.
     */
    @Transactional
    public Device registerForUser(Long userId,
                                  String rawFingerprint,
                                  String type,
                                  String os,
                                  String browser) {

        if (rawFingerprint == null || rawFingerprint.isBlank()) {
            throw new IllegalArgumentException("Fingerprint is required");
        }

        String trimmedFp = rawFingerprint.trim();
        var now = Instant.now();

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        // Is there already a device for this user? (1:1 enforced via UNIQUE(id))
        Device existing = repo.findByUserUseId(userId).orElse(null);

        boolean replaced = false;
        Device saved;

        if (existing == null) {
            // New device (fingerprint is the PK)
            String hashedFp = passwordEncoder.encode(trimmedFp);

            Device d = new Device();

            d.setFingerprint(hashedFp);
            d.setUser(user); // FK "id"

            d.setType(type);
            d.setOs(os);
            d.setBrowser(browser);
            d.setRegisteredAt(now);
            d.setLastLoginAt(now);

            saved = repo.saveAndFlush(d);

        } else if (passwordEncoder.matches(trimmedFp, existing.getFingerprint())) {
            // caso 2: es el MISMO dispositivo (mismo valor crudo), pero lo que hay en BD es un hash
            existing.setType(type);
            existing.setOs(os);
            existing.setBrowser(browser);
            existing.setLastLoginAt(now);

            saved = repo.saveAndFlush(existing);

        } else {
            // caso 3: había un dispositivo, pero la fp que llegó es distinta -> reemplazar
            // 1) Detach logs that point to the existing device
            logRepo.detachDeviceFromLogs(existing);

            // 2) Liberar referencias de SESSION (FK -> NULL) y borrar fila antigua
            try {
                sessionRepository.detachDeviceByFingerprint(existing.getFingerprint());
            } catch (Exception ignored) {
            }

            repo.delete(existing);
            repo.flush();

            String hashedFp = passwordEncoder.encode(trimmedFp);

            // 3) Insert new row with the new fingerprint
            Device device = new Device();
            device.setFingerprint(hashedFp);
            device.setUser(user);
            device.setType(type);
            device.setOs(os);
            device.setBrowser(browser);
            device.setRegisteredAt(now);
            device.setLastLoginAt(now);
            saved = repo.saveAndFlush(device);
            replaced = true;
        }

        // LOG FOR ACTION -----------------------------------------
        DeviceLog log = new DeviceLog();

        log.setUser(user);
        log.setDevice(saved);

        log.setDeviceFingerprintSnapshot(saved.getFingerprint());
        log.setDeviceOsSnapshot(saved.getOs());
        log.setDeviceTypeSnapshot(saved.getType());
        log.setDeviceBrowserSnapshot(saved.getBrowser());

        String action;
        String details;

        if (replaced) {
            action = "Replace";
            details = "Existing device replaced by new fingerprint";
        }

        else if (existing == null) {
            action = "Link";
            details = "Device linked successfully";
        }

        else {
            action = "Updated";
            details = "Device updated";
        }

        log.setAction(action);
        log.setResult("PLACEHOLDER");
        log.setDetails(details);

        log.setCreatedAt(now);
        logRepo.save(log);

        return saved;
    }

    @Transactional
    public void unlinkUserDevice(Long userId, String ip, String ua) {
        repo.findByUserUseId(userId).ifPresent(device -> {
            // (log moderno ya lo hicimos)
            DeviceLog log = new DeviceLog();
            log.setUser(userRepo.getReferenceById(userId));
            log.setDevice(device);
            log.setDeviceFingerprintSnapshot(device.getFingerprint());
            log.setDeviceOsSnapshot(device.getOs());
            log.setDeviceTypeSnapshot(device.getType());
            log.setDeviceBrowserSnapshot(device.getBrowser());
            log.setAction("Unlink");
            log.setResult("OK");
            log.setDetails("Device unlinked successfully");
            log.setCreatedAt(Instant.now());
            logRepo.saveAndFlush(log);

            logRepo.detachDeviceFromLogs(device);

            try {
                sessionRepository.detachDeviceByFingerprint(device.getFingerprint());
            } catch (Exception ignored) {}

            repo.delete(device);
            repo.flush();
        });
    }

    @Transactional(readOnly = true)
    public Optional<Device> findByUserId(Long userId) {
        return repo.findByUserUseId(userId);
    }

    @Transactional(readOnly = true)
    public Device ensureAuthorizedDevice(Long userId, String fpFromHeader) {

        Device device = repo.findByUserUseId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "DEVICE_REQUIRED"));

        System.out.println("FP header: " + fpFromHeader);
        System.out.println("FP db:     " + device.getFingerprint());
        System.out.println("matches?   " + passwordEncoder.matches(fpFromHeader, device.getFingerprint()));

        if (fpFromHeader == null || fpFromHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DEVICE_UNAUTHORIZED");
        }

        boolean ok = passwordEncoder.matches(fpFromHeader, device.getFingerprint());
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DEVICE_UNAUTHORIZED");
        }

        return device;
    }
}