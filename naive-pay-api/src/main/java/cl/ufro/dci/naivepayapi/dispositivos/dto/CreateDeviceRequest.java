package cl.ufro.dci.naivepayapi.dispositivos.dto;

/** Request para crear/registrar un Device. */
public record CreateDeviceRequest(
        String fingerprint,
        String type,
        String os,
        String browser
) {}
