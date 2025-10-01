package org.example.wordle.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity для хранения статистики игр в базе данных
 */
@Entity
@Table(name = "game_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStatsEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "game_date", nullable = false)
    private LocalDate gameDate;
    
    @Column(name = "attempts", nullable = false)
    private int attempts;
    
    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
    
    @Column(name = "game_time_seconds", nullable = false)
    private int gameTimeSeconds;
    
    @Column(name = "player_id", nullable = false, length = 255)
    private String playerId;
    
    @Column(name = "success", nullable = false)
    private boolean success;
    
    @Column(name = "target_word", nullable = false, length = 50)
    private String targetWord;
    
    /**
     * Конструктор для конвертации из GameStats
     */
    public GameStatsEntity(GameStats gameStats) {
        this.gameDate = gameStats.getGameDate();
        this.attempts = gameStats.getAttempts();
        this.completedAt = gameStats.getCompletedAt();
        this.gameTimeSeconds = gameStats.getGameTimeSeconds();
        this.playerId = gameStats.getPlayerId();
        this.success = gameStats.isSuccess();
        this.targetWord = gameStats.getTargetWord();
    }
    
    /**
     * Метод для конвертации в GameStats
     */
    public GameStats toGameStats() {
        return new GameStats(
            this.gameDate,
            this.attempts,
            this.completedAt,
            this.playerId,
            this.targetWord,
            this.gameTimeSeconds
        );
    }
}
