package cl.ufro.dci.naivepayapi.autentificacion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Tabla para guardar sesiones cerradas (historial).
 * NO tiene FK a Device ni User para evitar problemas al eliminar.
 */
@Entity
@Table(name = "session_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "his_id")
    private Long id;

    @Column(name = "his_jti", nullable = false)
    private String jti;

    // NO es FK, solo el ID del usuario
    @Column(name = "his_use_id", nullable = false)
    private Long userId;

    // NO es FK, solo snapshot del fingerprint
    @Column(name = "his_dev_fp", length = 255)
    private String deviceFingerprint;

    @Column(name = "his_created")
    private Instant created;

    @Column(name = "his_expires")
    private Instant expires;

    @Column(name = "his_closed")
    private Instant closed;

    @Column(name = "his_status", length = 20)
    private String status;

    // Razón del cierre: "logout", "device_replaced", "expired", etc.
    @Column(name = "his_close_reason", length = 50)
    private String closeReason;
}
