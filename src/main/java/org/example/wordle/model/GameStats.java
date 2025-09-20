package org.example.wordle.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Статистика игры в режиме дня
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStats {
    
    /**
     * Дата игры (для режима дня)
     */
    private LocalDate gameDate;
    
    /**
     * Количество попыток до угадывания (1-6, или 0 если не угадал)
     */
    private int attempts;
    
    /**
     * Время завершения игры
     */
    private LocalDateTime completedAt;
    
    /**
     * IP адрес игрока (для идентификации уникальных игроков)
     */
    private String playerId;
    
    /**
     * Была ли игра успешной
     */
    private boolean success;
    
    /**
     * Слово дня
     */
    private String targetWord;
    
    public GameStats(LocalDate gameDate, int attempts, LocalDateTime completedAt, String playerId, String targetWord) {
        this.gameDate = gameDate;
        this.attempts = attempts;
        this.completedAt = completedAt;
        this.playerId = playerId;
        this.success = attempts > 0 && attempts <= 6;
        this.targetWord = targetWord;
    }
}
