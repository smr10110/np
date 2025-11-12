package cl.ufro.dci.naivepayapi.dispositivos.domain;

import cl.ufro.dci.naivepayapi.registro.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a one-time recovery request for a user's device.
 * <p>
 * Stores the fingerprint of the device that initiated the recovery,
 * a six-digit verification code sent to the users mail, the current
 * status of the recovery process, and timestamps for when
 * the request was made, when it expires, and when it was verified
 */
@Entity
@Table(name = "device_recovery")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class DeviceRecovery {

    /** Unique identifier for this recovery request */
    @Id
    @Column(name = "dev_recover_id", nullable = false, updatable = false)
    private UUID id;

    /** User associated with this recovery request */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "useId",
            foreignKey = @ForeignKey(name = "fk_devrec_user")
    )
    private User user;

    /** Fingerprint of the device from which the recovery was requested. */
    @Column(name = "dev_rec_fp", nullable = false, length = 100)
    private String fingerprint;

    /** Six-digit recovery or verification code. */
    @Column(name = "dev_rec_code", nullable = false, length = 6)
    private String code;

    /** Current status of this recovery: PENDING, VERIFIED, EXPIRED, or CANCELED. */
    @Column(name = "dev_rec_status", nullable = false, length = 16)
    private String status;

    /** Time when the recovery was requested */
    @Column(name = "dev_rec_requested", nullable = false)
    private Instant requestedAt;

    /** Time when the recovery request expires */
    @Column(name = "dev_rec_expire", nullable = false)
    private Instant expiresAt;

    /** Time when the recovery was verified */
    @Column(name = "dev_rec_verified")
    private Instant verifiedAt;
}
