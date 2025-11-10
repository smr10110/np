package cl.ufro.dci.naivepayapi.autentificacion.domain;

import jakarta.persistence.*;
import lombok.*;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.SessionStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "use_id", nullable = false, referencedColumnName = "useId")
    private User user;

    // Permitimos NULL para conservar sesiones históricas si el Device se elimina
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "dev_fingerprint", referencedColumnName = "dev_fingerprint")
    private Device device;

    // Snapshot del fingerprint al momento de crear la sesión
    @Column(name = "ses_dev_fp", length = 255)
    private String sesDeviceFingerprint;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<AuthAttempt> attempts = new ArrayList<>();

    @Column(name = "ses_created", nullable = false)
    private Instant sesCreated;

    @Column(name = "ses_expires", nullable = false)
    private Instant sesExpires;

    @Column(name = "ses_closed")
    private Instant sesClosed;

    @Column(name = "ses_last_activity", nullable = false)
    private Instant sesLastActivity;

    @Column(name = "ses_max_expiration", nullable = false)
    private Instant sesMaxExpiration;

    @Enumerated(EnumType.STRING)
    @Column(name = "ses_status", nullable = false, length = 16)
    private SessionStatus status;
}