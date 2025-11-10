package cl.ufro.dci.naivepayapi.autentificacion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
import cl.ufro.dci.naivepayapi.registro.domain.User;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "use_id", referencedColumnName = "useId")
    private User user;

    // Snapshot del fingerprint del dispositivo al momento del intento
    @Column(name = "att_dev_fp", length = 255)
    private String attDeviceFingerprint;

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
}
