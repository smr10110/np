package cl.ufro.dci.naivepayapi.autentificacion.domain.enums;

public enum AuthAttemptReason {
    OK,
    USER_NOT_FOUND,
    BAD_CREDENTIALS,
    DEVICE_REQUIRED,
    DEVICE_UNAUTHORIZED
}