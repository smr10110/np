package cl.ufro.dci.naivepayapi.recompensas.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**representa la cuenta de recompensa de un usuario.
cada usuario tienen una cuenta de puntos que se acumulan y canjean.
*/
@Data
@Entity
public class RewardAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId; //usuario asociado
    private Integer points; //Puntos acumulados
    private String description;
    private LocalDateTime lastUpdate;

    public RewardAccount() {}

    public RewardAccount(Long userId, Integer points, String description, LocalDateTime lastUpdate) {
        this.userId = userId;
        this.points = points;
        this.description = description;
        this.lastUpdate = lastUpdate;
    }

    @PrePersist
    public void prePersist() {
        if (points == null) points = 0;
        lastUpdate = LocalDateTime.now();
    }

    public void addPoints(int pts) {
        this.points += pts;
        this.lastUpdate = LocalDateTime.now();
    }

    public void redeemPoints(int pts) {
        if (pts > points) throw new IllegalArgumentException("No hay suficientes puntos");
        this.points -= pts;
        this.lastUpdate = LocalDateTime.now();
    }
}