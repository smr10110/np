package cl.ufro.dci.naivepayapi.autentificacion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;

/**
 * AuthAttempt entity representing an authentication attempt
 * Follows the chain: Session -> AuthAttempt -> Device -> User
 * AuthAttempt has a relationship to Device, NOT directly to User
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "attempt_auth")
public class AuthAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "att_id")
    private Long attId;

    // Relación al Device que realizó el intento
    // Optional = true para permitir conservar intentos históricos si el Device se elimina
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "dev_fingerprint", referencedColumnName = "dev_fingerprint")
    private Device device;

    // Sesión a la que pertenece este intento (puede ser null si el intento falló antes de crear sesión)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "ses_id")
    private Session session;

    @Column(name = "att_success", nullable = false)
    private boolean attSuccess;

    @Enumerated(EnumType.STRING)
    @Column(name = "att_reason", nullable = false, length = 40)
    private AuthAttemptReason attReason;

    @Column(name = "att_occurred", nullable = false)
    private Instant attOccurred;

    // Método helper para obtener el User a través de Device
    public cl.ufro.dci.naivepayapi.registro.domain.User getUser() {
        return device != null ? device.getUser() : null;
    }
}
