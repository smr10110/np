package cl.ufro.dci.naivepayapi.registro.domain;

/**
 * Roles de usuario en el sistema NaivePay.
 *
 * - USER: Usuario normal (cliente)
 * - ADMIN: Administrador del sistema (puede validar comercios, etc.)
 */
public enum UserRole {
    USER,
    ADMIN
}
