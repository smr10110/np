package cl.ufro.dci.naivepayapi.autentificacion.domain;

import jakarta.persistence.*;
import lombok.*;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.SessionStatus;

import java.time.Instant;

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
    // ON DELETE CASCADE: Cuando se elimina el AuthAttempt (y por transitividad el Device),
    // se eliminan automáticamente todas las sesiones asociadas
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "att_id_initial",
        nullable = false,
        referencedColumnName = "att_id",
        foreignKey = @ForeignKey(
            name = "fk_session_auth_attempt",
            foreignKeyDefinition = "FOREIGN KEY (att_id_initial) REFERENCES attempt_auth(att_id) ON DELETE CASCADE"
        )
    )
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

    // Métodos helper para navegar la cadena
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