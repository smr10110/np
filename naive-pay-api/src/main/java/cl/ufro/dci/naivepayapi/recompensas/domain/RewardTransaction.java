package cl.ufro.dci.naivepayapi.recompensas.domain;

import cl.ufro.dci.naivepayapi.registro.domain.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table (name = "reward_transaction",
        indexes = { @Index(name = "idx_rewtrn_user", columnList = "user_id"),
                @Index(name = "idx_rewtrn_status", columnList = "rewtrn_status")})
public class RewardTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rewtrn_id")
    private Long rewtrnId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id",
            referencedColumnName = "useId",
            foreignKey = @ForeignKey(name = "fk_rewtrn_user"))
    private User user;

    @Column(name = "rewtrn_points", nullable = false)
    private Integer rewtrnPoints;

    @Enumerated(EnumType.STRING)
    @Column(name = "rewtrn_type", length = 20, nullable = false)
    private RewardTransactionType rewtrnType;

    @Enumerated(EnumType.STRING)
    @Column(name = "rewtrn_status", length = 20, nullable = false)
    private RewardTransactionStatus rewtrnStatus;

    @Column(name = "rewtrn_description", length = 255)
    private String rewtrnDescription;

    @Column(name = "rewtrn_created_at", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime rewtrnCreatedAt;

    @PrePersist
    public void prePersist() {
        this.rewtrnCreatedAt = LocalDateTime.now();
        if (rewtrnStatus == null) this.rewtrnStatus = RewardTransactionStatus.PENDING;
    }
}