package cl.ufro.dci.naivepayapi.dispositivos.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * One-to-many relationship: A user can have multiple device logs
 * Represents a historical record of actions performed on a users device
 * <p>
 * Each log entry captures snapshots of the device state and metadata at the time the event occurred.
 */
@Entity
@Table(name = "device_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DeviceLog {

    /** Unique identifier for the log entry */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "devlog_id")
    private Long id;

    /** User associated with the device action */
    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "useId",
                foreignKey = @ForeignKey(name = "fk_devlog_user"))
    private User user;

    /** Device involved in the logged event, if still present in the system. */
    @JsonIgnore
    @ManyToOne
    @JoinColumn(
            name = "dev_fingerprint",
            foreignKey = @ForeignKey(name = "fk_devlog_device")
    )
    private Device device;

    // ==================== Snapshots (to store the data of the device since we eliminate the devices when we replace them) ====================

    /** Fingerprint at the moment of the log */
    @Column(name = "devlog_fp_snapshot", length = 100)
    private String deviceFingerprintSnapshot;

    /** Os at the time of the log */
    @Column(name = "devlog_os_snapshot", length = 100)
    private String deviceOsSnapshot;

    /** Device type recorded at the time of the log */
    @Column(name = "devlog_type_snapshot", length = 50)
    private String deviceTypeSnapshot;

    /** Browser recorded at the time of the log */
    @Column(name = "devlog_browser_snapshot", length = 100)
    private String deviceBrowserSnapshot;

    // ==================== Event information ====================

    /** Type of action performed */
    @Column(name = "devlog_action", nullable = false, length = 25)
    private String action;

    /** Result status of the action */
    @Column(name = "devlog_result", nullable = false, length = 25)
    private String result;

    /** Additional details or context related to the logged event */
    @Column(name = "devlog_details", length = 100)
    private String details;

    /** Timestamp marking when the log was created */
    @Column(name = "devlog_created_at", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant createdAt;
}
