package cl.ufro.dci.naivepayapi.dispositivos.dto;

/**
 * Request body for verifying a device recovery code.
 * <p>
 * Includes the recovery request ID and the six-digit code
 * received by the user via email
 */
public record DeviceRecoverVerify(String recoveryId,
                                  String code) {}
