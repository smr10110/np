package cl.ufro.dci.naivepayapi.autentificacion.domain.enums;

public enum AuthAttemptReason {
    OK,
    PASSWORD_RESET,
    USER_NOT_FOUND,
    BAD_CREDENTIALS,
    EMAIL_NOT_VERIFIED,
    DEVICE_REQUIRED,
    DEVICE_UNAUTHORIZED,
    ACCOUNT_BLOCKED
}