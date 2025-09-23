package org.example.wordle.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Статистика дня для режима дня
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyStats {
    
    /**
     * Дата игры
     */
    private LocalDate gameDate;
    
    /**
     * Слово дня
     */
    private String targetWord;
    
    /**
     * Общее количество игроков, которые пытались угадать
     */
    private int totalPlayers;
    
    /**
     * Количество игроков, которые угадали слово
     */
    private int successfulPlayers;
    
    /**
     * Процент успешных игроков
     */
    private double successRate;
    
    /**
     * Распределение по количеству попыток
     * Ключ - количество попыток, Значение - количество игроков
     */
    private Map<Integer, Integer> attemptsDistribution;
    
    /**
     * Топ игроков (лучшие результаты)
     */
    private List<PlayerResult> topPlayers;
    
    /**
     * Результат конкретного игрока (если запрашивается)
     */
    private PlayerResult playerResult;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerResult {
        private String playerId;
        private int attempts;
        private LocalDateTime completedAt;
        private int rank; // Место в рейтинге (1, 2, 3...)
        private boolean success;
        private int gameTimeSeconds; // Время игры в секундах
    }
}
