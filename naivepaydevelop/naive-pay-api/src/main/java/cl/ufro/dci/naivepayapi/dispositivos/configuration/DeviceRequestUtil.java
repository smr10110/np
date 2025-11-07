package cl.ufro.dci.naivepayapi.dispositivos.configuration;

import cl.ufro.dci.naivepayapi.dispositivos.dto.DeviceRequestInfo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class DeviceRequestUtil {
    private static final String FP_HEADER = "X-Device-Fingerprint";

    public String fingerprintFromHeader(HttpServletRequest request) {
        String fp = request.getHeader(FP_HEADER);
        return (fp == null || fp.isBlank()) ? null : fp.trim();
    }

    public DeviceRequestInfo extract(HttpServletRequest request) {
        return new DeviceRequestInfo(fingerprintFromHeader(request));
    }

    /** Si quieres exigirla: */
    public String requireFingerprint(HttpServletRequest request) {
        String fp = fingerprintFromHeader(request);
        if (fp == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FINGERPRINT_REQUIRED");
        return fp;
    }
}
