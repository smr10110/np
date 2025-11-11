package cl.ufro.dci.naivepayapi.autentificacion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.AuthAttemptReason;
import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;

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

    // Relación al Device que realizó el intento
    // ON DELETE CASCADE: Cuando se elimina el Device, se eliminan automáticamente todos sus AuthAttempts
    // Esto mantiene la normalización: no duplicamos userId aquí, lo obtenemos de device.user.useId
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "dev_fingerprint",
        referencedColumnName = "dev_fingerprint",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_attempt_device",
            foreignKeyDefinition = "FOREIGN KEY (dev_fingerprint) REFERENCES device(dev_fingerprint) ON DELETE CASCADE"
        )
    )
    private Device device;

    @Column(name = "att_success", nullable = false)
    private boolean attSuccess;

    @Enumerated(EnumType.STRING)
    @Column(name = "att_reason", nullable = false, length = 40)
    private AuthAttemptReason attReason;

    @Column(name = "att_occurred", nullable = false)
    private Instant attOccurred;

    // Metodo helper para obtener el User a través de Device
    public cl.ufro.dci.naivepayapi.registro.domain.User getUser() {
        return device != null ? device.getUser() : null;
    }
}
