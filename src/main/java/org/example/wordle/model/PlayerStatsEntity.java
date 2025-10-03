package org.example.wordle.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_stats")
public class PlayerStatsEntity {
    
    @Id
    @Column(name = "player_id")
    private String playerId;
    
    @Column(name = "total_games", nullable = false)
    private Integer totalGames = 0;
    
    @Column(name = "total_wins", nullable = false)
    private Integer totalWins = 0;
    
    @Column(name = "win_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal winRate = BigDecimal.ZERO;
    
    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak = 0;
    
    @Column(name = "max_streak", nullable = false)
    private Integer maxStreak = 0;
    
    @Column(name = "average_attempts", precision = 3, scale = 2, nullable = false)
    private BigDecimal averageAttempts = BigDecimal.ZERO;
    
    @Column(name = "last_game_date")
    private LocalDate lastGameDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Конструкторы
    public PlayerStatsEntity() {}
    
    public PlayerStatsEntity(String playerId) {
        this.playerId = playerId;
    }
    
    // Геттеры и сеттеры
    public String getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public Integer getTotalGames() {
        return totalGames;
    }
    
    public void setTotalGames(Integer totalGames) {
        this.totalGames = totalGames;
    }
    
    public Integer getTotalWins() {
        return totalWins;
    }
    
    public void setTotalWins(Integer totalWins) {
        this.totalWins = totalWins;
    }
    
    public BigDecimal getWinRate() {
        return winRate;
    }
    
    public void setWinRate(BigDecimal winRate) {
        this.winRate = winRate;
    }
    
    public Integer getCurrentStreak() {
        return currentStreak;
    }
    
    public void setCurrentStreak(Integer currentStreak) {
        this.currentStreak = currentStreak;
    }
    
    public Integer getMaxStreak() {
        return maxStreak;
    }
    
    public void setMaxStreak(Integer maxStreak) {
        this.maxStreak = maxStreak;
    }
    
    public BigDecimal getAverageAttempts() {
        return averageAttempts;
    }
    
    public void setAverageAttempts(BigDecimal averageAttempts) {
        this.averageAttempts = averageAttempts;
    }
    
    public LocalDate getLastGameDate() {
        return lastGameDate;
    }
    
    public void setLastGameDate(LocalDate lastGameDate) {
        this.lastGameDate = lastGameDate;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
