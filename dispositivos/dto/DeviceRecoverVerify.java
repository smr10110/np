package cl.ufro.dci.naivepayapi.dispositivos.dto;

/** Payload para verificar el código recibido por correo. */
public record DeviceRecoverVerify(String recoveryId, String code) {}
