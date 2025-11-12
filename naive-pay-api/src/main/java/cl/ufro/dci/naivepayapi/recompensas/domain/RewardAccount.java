package cl.ufro.dci.naivepayapi.recompensas.domain;

import cl.ufro.dci.naivepayapi.registro.domain.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reward_account",
        indexes = { @Index(name = "idx_rewacc_user", columnList = "user_id")})
public class RewardAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rewacc_id")
    private Long rewaccId;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id",
            referencedColumnName = "useId",
            foreignKey = @ForeignKey(name = "fk_rewacc_user"))
    private User user; //usuario asociado

    @Column(name = "rewacc_points", nullable = false)
    private Integer rewaccPoints;
    @Column(name = "rewacc_description", length = 255)
    private String rewaccDescription;
    @Column(name = "rewacc_last_update", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime rewaccLastUpdate;

    public RewardAccount() {}

    public RewardAccount(User user, Integer points, String description, LocalDateTime lastUpdate) {
        this.user = user;
        this.rewaccPoints = points;
        this.rewaccDescription = description;
        this.rewaccLastUpdate = lastUpdate;
    }

    @PrePersist
    public void prePersist() {
        if (rewaccPoints == null) rewaccPoints = 0;
        rewaccLastUpdate = LocalDateTime.now();
    }

    public void addPoints(int pts) {
        this.rewaccPoints += pts;
        this.rewaccLastUpdate = LocalDateTime.now();
    }

    public void redeemPoints(int pts) {
        if (pts > rewaccPoints) throw new IllegalArgumentException("No hay suficientes puntos");
        this.rewaccPoints -= pts;
        this.rewaccLastUpdate = LocalDateTime.now();
    }
}