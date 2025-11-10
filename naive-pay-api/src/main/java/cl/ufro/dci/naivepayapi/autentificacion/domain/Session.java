package cl.ufro.dci.naivepayapi.autentificacion.domain;

import jakarta.persistence.*;
import lombok.*;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.SessionStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "att_id_initial", nullable = false, referencedColumnName = "att_id")
    private AuthAttempt initialAuthAttempt;

    // Todos los intentos de autenticación relacionados con esta sesión
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<AuthAttempt> attempts = new ArrayList<>();

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