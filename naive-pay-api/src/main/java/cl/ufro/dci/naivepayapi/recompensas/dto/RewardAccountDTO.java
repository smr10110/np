package cl.ufro.dci.naivepayapi.recompensas.dto;

import java.time.LocalDateTime;

public class RewardAccountDTO {
    private Long accountId;
    private Long userId;
    private Integer points;
    private String description;
    private LocalDateTime lastUpdate;

    public RewardAccountDTO() {}

    public RewardAccountDTO(Long accountId, Long userId, Integer points, String description, LocalDateTime lastUpdate) {
        this.accountId = accountId;
        this.userId = userId;
        this.points = points;
        this.description = description;
        this.lastUpdate = lastUpdate;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}