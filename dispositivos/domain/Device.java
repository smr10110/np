package cl.ufro.dci.naivepayapi.dispositivos.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import cl.ufro.dci.naivepayapi.registro.domain.User;

@Entity
@Table(
        name = "device",            //UniqueKey device user: exists to enforce 1:1 (
        uniqueConstraints = {@UniqueConstraint(name = "uk_dev_user", columnNames = {"id"})},
        indexes = {@Index(name = "idx_dev_fp", columnList = "dev_fingerprint")}
)

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor

public class Device {

    //Primary Key
    @Id
    @Column(name = "dev_fingerprint", length = 100, nullable = false)
    private String fingerprint;

    //Foregin Key id from user 1:1 relationship
    @OneToOne(optional = false)
    @JoinColumn(
            name = "id",
            referencedColumnName = "useId",
            foreignKey = @ForeignKey(name = "fk_dev_user")
    )
    private User user;

    // =========================================

    @Column(name = "dev_type", nullable = false, length = 100)
    private String type;

    @Column(name = "dev_os", nullable = false, length = 100)
    private String os;

    @Column(name = "dev_browser", nullable = false, length = 100)
    private String browser;

    @Column(name = "dev_reg_date", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant registeredAt;

    @Column(name = "dev_last_login", columnDefinition = "TIMESTAMP")
    private Instant lastLoginAt;
}
