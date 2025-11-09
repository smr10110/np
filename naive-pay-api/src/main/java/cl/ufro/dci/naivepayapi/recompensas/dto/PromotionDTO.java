package cl.ufro.dci.naivepayapi.recompensas.dto;

import java.time.LocalDateTime;

public class PromotionDTO {
    private String title;
    private String description;
    private int points;
    private String category;
    private LocalDateTime date;
    private LocalDateTime expiration;
    private String state;

    public PromotionDTO(String title, String description, int points, String categoria, LocalDateTime date, LocalDateTime expiration, String state) {
        this.title = title;
        this.description = description;
        this.points = points;
        this.category = category;
        this.expiration = expiration;
        this.state = state;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDateTime getExpiration() { return expiration; }
    public void setExpiration(LocalDateTime expiration) { this.date = expiration; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
