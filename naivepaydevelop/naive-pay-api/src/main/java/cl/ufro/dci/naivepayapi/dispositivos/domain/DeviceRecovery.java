package cl.ufro.dci.naivepayapi.dispositivos.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "device_recovery",
        indexes = {
                @Index(name = "idx_devrec_user", columnList = "user_id"),
                @Index(name = "idx_devrec_status", columnList = "status"),
                @Index(name = "idx_devrec_expires", columnList = "expires_at")
        })
public class DeviceRecovery {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Fingerprint desde la cual se intentó iniciar sesión. */
    @Column(nullable = false, length = 128)
    private String fingerprint;

    /** Código de 6 dígitos. */
    @Column(nullable = false, length = 6)
    private String code;

    /** PENDING | VERIFIED | EXPIRED | CANCELED */
    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    // Auditoría opcional
    @Column(length = 45)
    private String ip;

    @Column(length = 512)
    private String userAgent;
}
