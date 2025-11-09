package cl.ufro.dci.naivepayapi.recompensas.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**Representa una transaccion de recompensas*/
@Data
@Entity
public class RewardTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Integer points;

    @Enumerated(EnumType.STRING)
    private RewardTransactionType type;

    @Enumerated(EnumType.STRING)
    private RewardTransactionStatus status;

    private String description;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (status == null) this.status = RewardTransactionStatus.PENDING;
    }
}