package cl.ufro.dci.naivepayapi.dispositivos.dto;

/**
 * Permite iniciar la recuperación indicando el identificador del usuario:
 * email o RUT (con DV). Se usa cuando el login devuelve DEVICE_UNAUTHORIZED
 * o DEVICE_REQUIRED y el front necesita disparar el envío del código.
 */
public record DeviceRecoverRequest(String identifier) {}
