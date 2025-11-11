package cl.ufro.dci.naivepayapi.dispositivos.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import cl.ufro.dci.naivepayapi.registro.domain.User;

/**
 * Represents a device registered and linked to a specific user
 * <p>
 * Each user can have exactly one associated device (1:1 relationship)
 * The device is uniquely identified by its hashed fingerprint,
 * which acts as the primary key.
 * <p>
 * This entity stores metadata such as device type, OS, browser, registration date, and the timestamp of the last login.
 */
@Entity
@Table(name = "device")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Device {

    /**
     * Unique identifier (primary key) representing the device fingerprint.
     * <p>
     * This value is stored in a hashed form and used to identify the device securely during authentication.
     */
    @Id
    @Column(name = "dev_fingerprint", length = 100, nullable = false)
    private String fingerprint;

    /**
     * Reference to the user who owns this device.
     * <p>
     * This establishes a one-to-one (1:1) relationship where each user can have AT MOST ONE REGISTERED DEVICE.
     * The relationship is enforced through the foreign key {@code fk_dev_user}.
     */
    @OneToOne(optional = false)
    @JoinColumn(
            name = "use_id",
            foreignKey = @ForeignKey(name = "fk_dev_user"))
    private User user;

    /**
     * Type of device
     */
    @Column(name = "dev_type", nullable = false, length = 100)
    private String type;

    /**
     * Operating system of the device
     */
    @Column(name = "dev_os", nullable = false, length = 100)
    private String os;

    /**
     * Browser or client used for access
     */
    @Column(name = "dev_browser", nullable = false, length = 100)
    private String browser;

    /**
     * Date and time when the device was first registered
     */
    @Column(name = "dev_reg_date", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant registeredAt;

    /**
     * Timestamp of the most recent login from the device
     */
    @Column(name = "dev_last_login", columnDefinition = "TIMESTAMP")
    private Instant lastLoginAt;
}
