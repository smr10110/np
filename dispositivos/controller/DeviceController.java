package cl.ufro.dci.naivepayapi.dispositivos.controller;


import cl.ufro.dci.naivepayapi.dispositivos.configuration.DeviceTokenUtil;
import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import cl.ufro.dci.naivepayapi.dispositivos.domain.DeviceLog;
import cl.ufro.dci.naivepayapi.dispositivos.dto.CreateDeviceRequest;
import cl.ufro.dci.naivepayapi.dispositivos.dto.DeviceRecoverRequest;
import cl.ufro.dci.naivepayapi.dispositivos.dto.DeviceRecoverVerify;
import cl.ufro.dci.naivepayapi.dispositivos.repository.DeviceLogRepository;
import cl.ufro.dci.naivepayapi.dispositivos.service.DeviceService;
import cl.ufro.dci.naivepayapi.dispositivos.service.DeviceRecoveryService;
import cl.ufro.dci.naivepayapi.autentificacion.dto.LoginResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = {"/api/devices", "/api/dispositivos"}) // Gotta change this eventually
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceLogRepository logRepo;
    private final DeviceTokenUtil deviceTokenUtil;
    private final DeviceRecoveryService deviceRecoveryService;

    @PostMapping("/link")
    public ResponseEntity<Device> linkDevice(@RequestBody CreateDeviceRequest body, HttpServletRequest request) {
        Long userId = deviceTokenUtil.getUserId(request);

        String fingerprint = firstNonBlank(
                nullSafe(body.fingerprint()),
                deviceTokenUtil.getDeviceFingerprintFromHeader(request) // temporary plan B
        );
        if (isBlank(fingerprint)) {
            throw new IllegalArgumentException("Device fingerprint is required (body or X-Device-Fingerprint header)");
        }

        String type = firstNonBlank(nullSafe(body.type()), "Unknown");
        String os   = firstNonBlank(nullSafe(body.os()), "N/A");
        String browser = firstNonBlank(nullSafe(body.browser()), "Device");

        String ip = clientIp(request);
        String ua = Optional.ofNullable(request.getHeader("User-Agent")).orElse("N/A");

        deviceService.findByUserId(userId).ifPresent(existing -> deviceService.unlinkUserDevice(userId, ip, ua));
        Device device = deviceService.registerForUser(userId, fingerprint, type, os, browser);

        return ResponseEntity
                .created(URI.create("/api/devices/" + device.getFingerprint()))
                .body(device);
    }

    @DeleteMapping("/unlink")
    public ResponseEntity<?> unlinkDevice(HttpServletRequest req) {
        Long userId = getAuthenticatedUserId();
        String fp = req.getHeader("X-Device-Fingerprint");
        deviceService.ensureAuthorizedDevice(userId, fp);
        deviceService.unlinkUserDevice(
                userId,
                req.getRemoteAddr(),
                req.getHeader("User-Agent")
        );
        return ResponseEntity.ok(Map.of("message", "Device unlink successfully"));
    }

    @GetMapping("/current")
    public ResponseEntity<?> getDeviceByUser(HttpServletRequest request) {
        Long userId = deviceTokenUtil.getUserId(request);
        return deviceService.findByUserId(userId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Map.of("message","No device linked currently")));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<DeviceLog>> getLogs(HttpServletRequest req) {
        Long userId = getAuthenticatedUserId();
        String fp = req.getHeader("X-Device-Fingerprint");
        deviceService.ensureAuthorizedDevice(userId, fp);
        List<DeviceLog> logs = logRepo.findByUserUseId(userId);
        return ResponseEntity.ok(logs);
    }

    /**
     * Starts recovery: sends a code to the user's email and returns the recoveryId.
     * Body: { "identifier": "<email or national_id>" }
     * Required header: X-Device-Fingerprint
     */
    @PostMapping("/recover/request")
    public ResponseEntity<?> startRecovery(@RequestBody DeviceRecoverRequest body, HttpServletRequest request) {
        String fp = deviceTokenUtil.getDeviceFingerprintFromHeader(request);
        if (isBlank(fp)) {
            return ResponseEntity.badRequest().body(Map.of("error", "FINGERPRINT_REQUIRED"));
        }
        String ip = clientIp(request);
        String ua = Optional.ofNullable(request.getHeader("User-Agent")).orElse("N/A");

        var rec = deviceRecoveryService.start(body.identifier(), fp, ip, ua);
        return ResponseEntity.ok(Map.of(
                "message", "RECOVERY_SENT",
                "recoveryId", rec.getId().toString()
        ));
    }

    /**
     * Verifies the code and links the current device; returns a LoginResponse with token.
     * Body: { "recoveryId": "...", "code": "123456" }
     * Required header: X-Device-Fingerprint
     */
    @PostMapping("/recover/verify")
    public ResponseEntity<?> verifyRecovery(@RequestBody DeviceRecoverVerify body, HttpServletRequest request) {
        String fp = deviceTokenUtil.getDeviceFingerprintFromHeader(request);
        if (isBlank(fp)) {
            return ResponseEntity.badRequest().body(Map.of("error", "FINGERPRINT_REQUIRED"));
        }

        // 👇 read extra headers
        String os = request.getHeader("X-Device-OS");
        String type = request.getHeader("X-Device-Type");
        String browser = request.getHeader("X-Device-Browser");

        LoginResponse resp = deviceRecoveryService.verifyAndLink(
                body.recoveryId(),
                body.code(),
                fp,
                firstNonBlank(type, "DESKTOP"),
                firstNonBlank(os, "N/A"),
                firstNonBlank(browser, "Unknown")
        );
        return ResponseEntity.ok(resp);
    }

    //========================== helpers ======================================

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            if (parts.length > 0) return parts[0].trim();
        }
        return request.getRemoteAddr();
    }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String nullSafe(String s) { return s == null ? null : s.trim(); }
    private static String firstNonBlank(String a, String b) { return !isBlank(a) ? a : b; }

    private Long getAuthenticatedUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return Long.valueOf(auth.getPrincipal().toString());
    }

}
