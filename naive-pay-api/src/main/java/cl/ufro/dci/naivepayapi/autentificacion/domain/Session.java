package cl.ufro.dci.naivepayapi.autentificacion.domain;

import jakarta.persistence.*;
import lombok.*;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.SessionStatus;

import java.time.Instant;

/**
 * Session entity representing a user session
 * Follows the chain: Session -> AuthAttempt -> Device -> User
 * Session does NOT have direct relationships to User or Device
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "session")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ses_id")
    private Long sesId;

    @Column(name = "ses_jti", nullable = false, unique = true)
    private java.util.UUID sesJti;

    // Relación al AuthAttempt que inició esta sesión
    // NOTA: nullable = true para soportar sesiones huérfanas de migraciones/refactorizaciones anteriores
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "att_id_initial", nullable = true, referencedColumnName = "att_id")
    private AuthAttempt initialAuthAttempt;

    @Column(name = "ses_created", nullable = false)
    private Instant sesCreated;

    @Column(name = "ses_expires", nullable = false)
    private Instant sesExpires;

    @Column(name = "ses_closed")
    private Instant sesClosed;

    @Enumerated(EnumType.STRING)
    @Column(name = "ses_status", nullable = false, length = 16)
    private SessionStatus status;

    // Métodos helper para navegar la cadena Session -> AuthAttempt -> Device -> User
    public cl.ufro.dci.naivepayapi.dispositivos.domain.Device getDevice() {
        return initialAuthAttempt != null ? initialAuthAttempt.getDevice() : null;
    }

    public cl.ufro.dci.naivepayapi.registro.domain.User getUser() {
        if (initialAuthAttempt != null && initialAuthAttempt.getDevice() != null) {
            return initialAuthAttempt.getDevice().getUser();
        }
        return null;
    }
}