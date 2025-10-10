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
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlayerStatsMigrationService {
    
    @Autowired
    private GameStatsRepository gameStatsRepository;
    
    @Autowired
    private PlayerStatsRepository playerStatsRepository;
    
    /**
     * Мигрирует данные из game_stats в player_stats
     */
    @Transactional
    public void migratePlayerStats() {
        System.out.println("🔄 Начинаем миграцию персональной статистики...");
        
        // Получаем всех игроков, которые играли
        List<GameStatsEntity> allGames = gameStatsRepository.findAll();
        Map<String, List<GameStatsEntity>> gamesByPlayer = allGames.stream()
                .collect(Collectors.groupingBy(GameStatsEntity::getPlayerId));
        
        System.out.println("📊 Найдено игроков: " + gamesByPlayer.size());
        
        int migratedCount = 0;
        
        for (Map.Entry<String, List<GameStatsEntity>> entry : gamesByPlayer.entrySet()) {
            String playerId = entry.getKey();
            List<GameStatsEntity> playerGames = entry.getValue();
            
            // Сортируем игры по дате
            playerGames.sort((a, b) -> a.getGameDate().compareTo(b.getGameDate()));
            
            // Создаем или обновляем статистику игрока
            PlayerStatsEntity playerStats = createOrUpdatePlayerStats(playerId, playerGames);
            playerStatsRepository.save(playerStats);
            
            migratedCount++;
            System.out.println("✅ Мигрирована статистика для игрока " + playerId + 
                             " (игр: " + playerGames.size() + ")");
        }
        
        System.out.println("🎉 Миграция завершена! Обработано игроков: " + migratedCount);
    }
    
    /**
     * Создает или обновляет статистику игрока на основе его игр
     */
    private PlayerStatsEntity createOrUpdatePlayerStats(String playerId, List<GameStatsEntity> games) {
        PlayerStatsEntity stats = new PlayerStatsEntity(playerId);
        
        // Подсчитываем общую статистику
        int totalGames = games.size();
        int totalWins = (int) games.stream().filter(GameStatsEntity::isSuccess).count();
        int totalAttempts = games.stream().mapToInt(GameStatsEntity::getAttempts).sum();
        
        // Рассчитываем процент побед
        BigDecimal winRate = totalGames > 0 ? 
            BigDecimal.valueOf(totalWins)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalGames), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
        
        // Рассчитываем среднее количество попыток
        BigDecimal averageAttempts = totalGames > 0 ?
            BigDecimal.valueOf(totalAttempts)
                .divide(BigDecimal.valueOf(totalGames), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
        
        // Рассчитываем стрики
        int currentStreak = 0;
        int maxStreak = 0;
        LocalDate lastGameDate = null;
        
        for (GameStatsEntity game : games) {
            if (game.isSuccess()) {
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
        
        // Устанавливаем значения
        stats.setTotalGames(totalGames);
        stats.setTotalWins(totalWins);
        stats.setWinRate(winRate);
        stats.setCurrentStreak(currentStreak);
        stats.setMaxStreak(maxStreak);
        stats.setAverageAttempts(averageAttempts);
        stats.setLastGameDate(lastGameDate);
        
        return stats;
    }
    
    /**
     * Проверяет, нужна ли миграция
     */
    public boolean needsMigration() {
        long playerStatsCount = playerStatsRepository.count();
        long gameStatsCount = gameStatsRepository.count();
        
        // Если в player_stats нет записей, а в game_stats есть - нужна миграция
        return playerStatsCount == 0 && gameStatsCount > 0;
    }
}
