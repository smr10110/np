package cl.ufro.dci.naivepayapi.dispositivos.dto;

/**
 * Request body for linking or registering a new device
 * <p>
 * Contains identifying information about the device and its environment, provided by the client during registration
 */
public record CreateDeviceRequest(
        String fingerprint,
        String type,
        String os,
        String browser
) {}
