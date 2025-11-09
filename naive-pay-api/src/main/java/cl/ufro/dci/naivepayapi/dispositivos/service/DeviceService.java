package cl.ufro.dci.naivepayapi.dispositivos.service;

import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import cl.ufro.dci.naivepayapi.dispositivos.domain.DeviceLog;
import cl.ufro.dci.naivepayapi.dispositivos.repository.DeviceLogRepository;
import cl.ufro.dci.naivepayapi.dispositivos.repository.DeviceRepository;
import cl.ufro.dci.naivepayapi.autentificacion.repository.SessionRepository;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository devRepo;
    private final DeviceLogRepository devLogRepo;
    private final UserRepository userRepo;
    private final SessionRepository sessionRepo;
    private final PasswordEncoder passEncoderService;

    /**
     * Registers a new device for a given user, or replaces an existing one
     * <p>
     * If the user does not have a registered device, a new one is created.
     * If a device already exists, it is safely replaced after detaching its reference from logs and sessions
     *
     * @param userId         the id of the user to register the device for
     * @param rawFingerprint the raw fingerprint identifying the device
     * @param type           the device type
     * @param os             the operating system
     * @param browser        the browser
     *
     * @return the new {@link Device}
     * @throws IllegalArgumentException if the fingerprint is null or blank
     * @throws ResponseStatusException  if the user does not exist
     */
    @Transactional
    public Device registerForUser(Long userId,
                                  String rawFingerprint,
                                  String type,
                                  String os,
                                  String browser) {

        String logResult = "PLACEHOLDER";

        if (rawFingerprint == null || rawFingerprint.isBlank())
            throw new IllegalArgumentException("Fingerprint is required");

        User user = userRepo.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        String normalizedFingerprint = rawFingerprint.trim();
        Instant now = Instant.now();

        Device existingDevice = devRepo.findByUserUseId(userId).orElse(null);
        Device newDevice;

        if (existingDevice == null) {
            newDevice = createDevice(user, normalizedFingerprint, type, os, browser, now);
        } else {
            newDevice = replaceDevice(user, existingDevice, normalizedFingerprint, type, os, browser, now);
        }

        createDeviceLog(user, newDevice, existingDevice, logResult, now);
        return newDevice;
    }

    /**
     * Creates a new device entry for the given user.
     *
     * @param user        the associated user
     * @param fingerprint the normalized fingerprint string
     * @param type        the device type
     * @param os          the operating system name
     * @param browser     the browser name
     * @param now         the current timestamp
     * @return the new {@link Device}
     */
    private Device createDevice(User user,
                                String fingerprint,
                                String type,
                                String os,
                                String browser,
                                Instant now) {

        String hashedFingerprint = passEncoderService.encode(fingerprint); // hash the fingerprint

        Device device = new Device();
        device.setUser(user);
        device.setFingerprint(hashedFingerprint);

        device.setType(type);
        device.setOs(os);
        device.setBrowser(browser);
        device.setRegisteredAt(now);
        device.setLastLoginAt(now);

        return devRepo.saveAndFlush(device);
    }

    /**
     * Replaces an existing device by removing it and creating a new one.
     * All related logs and sessions are detached before deletion to preserve integrity.
     *
     * @param user           the associated user
     * @param oldDevice the device currently linked to the user
     * @param fingerprint    the new fingerprint to register
     * @param type           the device type
     * @param os             the operating system name
     * @param browser        the browser name
     * @param now            the current timestamp
     * @return the new {@link Device}
     */
    private Device replaceDevice(User user,
                                 Device oldDevice,
                                 String fingerprint,
                                 String type,
                                 String os,
                                 String browser,
                                 Instant now) {

        devLogRepo.detachDeviceFromLogs(oldDevice);
        try {
            sessionRepo.detachDeviceByFingerprint(oldDevice.getFingerprint());
        } catch (Exception ignored) {}

        devRepo.delete(oldDevice);
        devRepo.flush();

        return createDevice(user, fingerprint, type, os, browser, now);
    }


    /**
     * Creates the corresponding log entries for a device registration or replacement.
     * <p>
     * If a previous device existed, an "Unlink" action is recorded before the new "Link".
     *
     * @param user           the user performing the registration
     * @param newDevice      the new registered device
     * @param oldDevice      the previously linked device, if any
     * @param logResult      the outcome for logging
     * @param now            the timestamp of the operation
     */
    private void createDeviceLog(User user,
                                 Device newDevice,
                                 Device oldDevice,
                                 String logResult,
                                 Instant now) {

        if (oldDevice != null) {
            logDeviceAction(user, oldDevice, "Unlink", logResult, "Previous device unlinked due to new registration", now);
        }
        logDeviceAction(user, newDevice, "Link", logResult, "Device linked successfully", now);
    }


    /**
     * Logs a device action ("Link" or "Unlink")
     * <p>
     * If the device no longer exists (after unlink), only its snapshot data is stored.
     *
     * @param user       user linked to the action
     * @param newDevice     device involved (can be deleted or active)
     * @param action     action performed ("Link" or "Unlink")
     * @param logResult     operation result (e.g. "OK")
     * @param details    human-readable description
     * @param createdAt  timestamp of the log
     */
    private void logDeviceAction(User user,
                                 Device newDevice,
                                 String action,
                                 String logResult,
                                 String details,
                                 Instant createdAt) {

        DeviceLog log = new DeviceLog();
        log.setUser(user);

        if ("Link".equalsIgnoreCase(action)) {
            log.setDevice(newDevice);
        }
        populateLogFields(log, newDevice, action, logResult, details, createdAt);
        devLogRepo.save(log);
    }

    /**
     * Fills in common {@link DeviceLog} fields using data from a {@link Device}.
     *
     * @param log       the log entry to fill
     * @param device    source device (for snapshots)
     * @param action    log action
     * @param logResult log result
     * @param details   log details
     * @param createdAt timestamp of creation
     */
    private void populateLogFields(DeviceLog log,
                                   Device device,
                                   String action,
                                   String logResult,
                                   String details,
                                   Instant createdAt) {

        log.setDeviceFingerprintSnapshot(device.getFingerprint());
        log.setDeviceOsSnapshot(device.getOs());
        log.setDeviceTypeSnapshot(device.getType());
        log.setDeviceBrowserSnapshot(device.getBrowser());
        log.setAction(action);
        log.setResult(logResult);
        log.setDetails(details);
        log.setCreatedAt(createdAt);
    }

    /**
     * Unlinks and deletes the device associated with a user.
     * <p>
     * Before deletion, all log and session references are detached
     * and an "Unlink" action is recorded.
     *
     * @param userId ID of the user whose device should be unlinked
     */
    @Transactional
    public void unlinkUserDevice(Long userId) {
        devRepo.findByUserUseId(userId).ifPresent(device -> {
            User user = userRepo.getReferenceById(userId);
            Instant now = Instant.now();

            logDeviceAction(user, device, "Unlink", "OK", "Device unlinked successfully", now);

            devLogRepo.detachDeviceFromLogs(device);

            try {
                sessionRepo.detachDeviceByFingerprint(device.getFingerprint());
            } catch (Exception ignored) {}

            devRepo.delete(device);
            devRepo.flush();
        });
    }

    // ====================================== Utilities ======================================

    /**
     * Finds a device linked to a specific user
     *
     * @param userId the ID of the user
     *
     * @return an {@link Optional} containing the device, if found
     */
    @Transactional(readOnly = true)
    public Optional<Device> findByUserId(Long userId) {
        return devRepo.findByUserUseId(userId);
    }

    /**
     * Ensures a device is authorized for the given user
     * Verifies the fingerprint hash against the stored hash
     *
     * @param userId       the ID of the user
     * @param fpFromHeader the raw fingerprint from the request header
     *
     * @return the authorized {@link Device} entity
     * @throws ResponseStatusException if the fingerprint is invalid or the device is missing
     */
    @Transactional(readOnly = true)
    public Device ensureAuthorizedDevice(Long userId, String fpFromHeader) {
        Device device = devRepo.findByUserUseId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "DEVICE_REQUIRED"));

        if (fpFromHeader == null || fpFromHeader.isBlank())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DEVICE_UNAUTHORIZED");

        boolean ok = passEncoderService.matches(fpFromHeader, device.getFingerprint());
        if (!ok) // ok
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DEVICE_UNAUTHORIZED");

        return device;
    }
}
