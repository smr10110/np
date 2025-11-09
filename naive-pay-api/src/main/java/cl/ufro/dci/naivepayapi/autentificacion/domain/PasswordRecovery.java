package cl.ufro.dci.naivepayapi.autentificacion.domain;

import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.PasswordRecoveryStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import cl.ufro.dci.naivepayapi.registro.domain.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "password_recovery")
public class PasswordRecovery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pas_id")
    private Long pasId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "use_id", nullable = false)
    private User user;

    @Column(name = "pas_code", nullable = false, length = 40)
    private String pasCode;

    @Column(name = "pas_created", nullable = false)
    private Instant pasCreated;

    @Column(name = "pas_expired", nullable = false)
    private Instant pasExpired;

    @Column(name = "pas_last_sent")
    private Instant pasLastSent;

    @Column(name = "pas_used")
    private Instant pasUsed;

    @Column(name = "pas_resend_count", nullable = false)
    private int pasResendCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "pas_status", nullable = false, length = 16)
    private PasswordRecoveryStatus pasStatus;
}