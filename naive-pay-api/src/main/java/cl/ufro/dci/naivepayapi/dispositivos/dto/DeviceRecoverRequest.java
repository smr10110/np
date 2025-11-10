package cl.ufro.dci.naivepayapi.dispositivos.dto;

/**
 * Request body for starting a device recovery process.
 * <p>
 * The identifier can be either an email or a national ID (RUT)
 * used to locate the user requesting the recovery.
 */
public record DeviceRecoverRequest(String identifier) {}
