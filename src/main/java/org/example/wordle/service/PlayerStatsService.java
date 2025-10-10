package org.example.wordle.service;

import org.example.wordle.model.GameStatsEntity;
import org.example.wordle.model.PlayerStatsEntity;
import org.example.wordle.repository.GameStatsRepository;
import org.example.wordle.repository.PlayerStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PlayerStatsService {
    
    @Autowired
    private PlayerStatsRepository playerStatsRepository;
    
    @Autowired
    private GameStatsRepository gameStatsRepository;
    
    /**
     * Получить или создать статистику игрока
     */
    public PlayerStatsEntity getOrCreatePlayerStats(String playerId) {
        Optional<PlayerStatsEntity> existingStats = playerStatsRepository.findByPlayerId(playerId);
        if (existingStats.isPresent()) {
            return existingStats.get();
        }
        
        // Создаем новую запись статистики
        PlayerStatsEntity newStats = new PlayerStatsEntity(playerId);
        return playerStatsRepository.save(newStats);
    }
    
    /**
     * Обновить статистику игрока после завершения игры
     */
    @Transactional
    public void updatePlayerStats(String playerId, boolean won, int attempts, LocalDate gameDate) {
        PlayerStatsEntity stats = getOrCreatePlayerStats(playerId);
        
        // Обновляем общие счетчики
        stats.setTotalGames(stats.getTotalGames() + 1);
        if (won) {
            stats.setTotalWins(stats.getTotalWins() + 1);
        }
        
        // Пересчитываем процент побед
        BigDecimal winRate = BigDecimal.valueOf(stats.getTotalWins())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(stats.getTotalGames()), 2, RoundingMode.HALF_UP);
        stats.setWinRate(winRate);
        
        // Обновляем стрик
        updateStreak(stats, won, gameDate);
        
        // Пересчитываем среднее количество попыток
        recalculateAverageAttempts(stats);
        
        // Обновляем дату последней игры
        stats.setLastGameDate(gameDate);
        
        playerStatsRepository.save(stats);
    }
    
    /**
     * Обновить стрик игрока
     * Стрик обнуляется только при проигрыше, а не при пропуске дней
     */
    private void updateStreak(PlayerStatsEntity stats, boolean won, LocalDate gameDate) {
        if (won) {
            // При выигрыше увеличиваем стрик
            stats.setCurrentStreak(stats.getCurrentStreak() + 1);
            
            // Обновляем максимальный стрик
            if (stats.getCurrentStreak() > stats.getMaxStreak()) {
                stats.setMaxStreak(stats.getCurrentStreak());
            }
        } else {
            // При проигрыше обнуляем стрик
            stats.setCurrentStreak(0);
        }
    }
    
    /**
     * Пересчитать среднее количество попыток
     */
    private void recalculateAverageAttempts(PlayerStatsEntity stats) {
        List<GameStatsEntity> playerGames = gameStatsRepository.findByPlayerIdOrderByCompletedAtAsc(stats.getPlayerId());
        
        if (playerGames.isEmpty()) {
            stats.setAverageAttempts(BigDecimal.ZERO);
            return;
        }
        
        int totalAttempts = playerGames.stream()
                .mapToInt(GameStatsEntity::getAttempts)
                .sum();
        
        BigDecimal averageAttempts = BigDecimal.valueOf(totalAttempts)
                .divide(BigDecimal.valueOf(playerGames.size()), 2, RoundingMode.HALF_UP);
        
        stats.setAverageAttempts(averageAttempts);
    }
    
    /**
     * Получить статистику игрока
     */
    public PlayerStatsEntity getPlayerStats(String playerId) {
        return getOrCreatePlayerStats(playerId);
    }
    
    /**
     * Пересчитать статистику игрока на основе всех его игр
     */
    @Transactional
    public void recalculatePlayerStats(String playerId) {
        List<GameStatsEntity> playerGames = gameStatsRepository.findByPlayerIdOrderByCompletedAtAsc(playerId);
        
        if (playerGames.isEmpty()) {
            return;
        }
        
        PlayerStatsEntity stats = getOrCreatePlayerStats(playerId);
        
        // Сбрасываем статистику
        stats.setTotalGames(0);
        stats.setTotalWins(0);
        stats.setCurrentStreak(0);
        stats.setMaxStreak(0);
        stats.setAverageAttempts(BigDecimal.ZERO);
        stats.setLastGameDate(null);
        
        // Пересчитываем на основе всех игр
        int currentStreak = 0;
        int maxStreak = 0;
        int totalAttempts = 0;
        LocalDate lastGameDate = null;
        
        for (GameStatsEntity game : playerGames) {
            stats.setTotalGames(stats.getTotalGames() + 1);
            totalAttempts += game.getAttempts();
            
            if (game.isSuccess()) {
                stats.setTotalWins(stats.getTotalWins() + 1);
                
                // При выигрыше увеличиваем стрик (независимо от пропусков дней)
                currentStreak++;
                
                if (currentStreak > maxStreak) {
                    maxStreak = currentStreak;
                }
            } else {
                // При проигрыше обнуляем стрик
                currentStreak = 0;
            }
            
            lastGameDate = game.getGameDate();
        }
        
        // Устанавливаем финальные значения
        stats.setCurrentStreak(currentStreak);
        stats.setMaxStreak(maxStreak);
        stats.setLastGameDate(lastGameDate);
        
        // Пересчитываем процент побед
        if (stats.getTotalGames() > 0) {
            BigDecimal winRate = BigDecimal.valueOf(stats.getTotalWins())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(stats.getTotalGames()), 2, RoundingMode.HALF_UP);
            stats.setWinRate(winRate);
        }
        
        // Пересчитываем среднее количество попыток
        if (stats.getTotalGames() > 0) {
            BigDecimal averageAttempts = BigDecimal.valueOf(totalAttempts)
                    .divide(BigDecimal.valueOf(stats.getTotalGames()), 2, RoundingMode.HALF_UP);
            stats.setAverageAttempts(averageAttempts);
        }
        
        playerStatsRepository.save(stats);
    }
}
