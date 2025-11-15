package cl.ufro.dci.naivepayapi.recompensas.dto;

import cl.ufro.dci.naivepayapi.recompensas.domain.RewardTransaction;
import cl.ufro.dci.naivepayapi.recompensas.domain.RewardTransactionStatus;
import cl.ufro.dci.naivepayapi.recompensas.domain.RewardTransactionType;

import java.time.LocalDateTime;

public class RewardTransactionDTO {
    private Long id;
    private Long userId;
    private int points;
    private String description;
    private RewardTransactionType type;
    private RewardTransactionStatus status;
    private LocalDateTime createdAt;

    public RewardTransactionDTO(RewardTransaction transaction) {
        this.id = transaction.getRewtrnId();
        this.userId = transaction.getUser().getUseId(); // <--- Corregido
        this.points = transaction.getRewtrnPoints();
        this.description = transaction.getRewtrnDescription();
        this.type = transaction.getRewtrnType();
        this.status = transaction.getRewtrnStatus();
        this.createdAt = transaction.getRewtrnCreatedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public RewardTransactionType getType() { return type; }
    public void setType(RewardTransactionType type) { this.type = type; }
    public RewardTransactionStatus getStatus() { return status; }
    public void setStatus(RewardTransactionStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}