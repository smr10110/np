package cl.ufro.dci.naivepayapi.dispositivos.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "device_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "devlog_id")
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "useId")
    private User user;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "dev_fingerprint")
    private Device device;

    // --- snapshots of the device the moment of log creation ---

    @Column(name = "devlog_fp_snapshot", length = 100)
    private String deviceFingerprintSnapshot;

    @Column(name = "devlog_os_snapshot", length = 100)
    private String deviceOsSnapshot;

    @Column(name = "devlog_type_snapshot", length = 50)
    private String deviceTypeSnapshot;

    @Column(name = "devlog_browser_snapshot", length = 100)
    private String deviceBrowserSnapshot;

    //  Event info --------------------------------------------

    @Column(name = "devlog_action", nullable = false, length = 25)
    private String action;

    @Column(name = "devlog_result", nullable = false, length = 25)
    private String result;

    @Column(name = "devlog_details", length = 100)
    private String details;

    @Column(name = "devlog_created_at", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant createdAt;
}
