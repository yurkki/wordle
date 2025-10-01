package org.example.wordle.service;

import org.example.wordle.model.DailyStats;
import org.example.wordle.model.GameStats;
import org.example.wordle.model.GameStatsEntity;
import org.example.wordle.repository.GameStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для сбора и анализа статистики игр
 */
@Service
public class StatsService {
    
    @Autowired
    private GameStatsRepository gameStatsRepository;
    
    @Autowired
    private LocalTimeService localTimeService;
    
    @Autowired
    private DailyGameValidationService dailyGameValidationService;
    
    /**
     * Записывает статистику завершенной игры в базу данных
     * Проверяет, что игрок еще не играл сегодня в режиме дня
     */
    public boolean recordGameStats(LocalDate gameDate, int attempts, String playerId, String targetWord, int gameTimeSeconds) {
        // Проверяем, что игрок еще не играл сегодня
        if (!dailyGameValidationService.canPlayerPlayToday(playerId)) {
            System.out.println("❌ Игрок " + playerId + " уже играл сегодня, статистика не записывается");
            return false;
        }
        
        // Проверяем, что время валидно для игры в режиме дня
        if (!dailyGameValidationService.isValidTimeForDailyGame()) {
            System.out.println("❌ Время не подходит для игры в режиме дня, статистика не записывается");
            return false;
        }
        
        // Дополнительная проверка: убеждаемся, что targetWord не пустое
        if (targetWord == null || targetWord.trim().isEmpty()) {
            System.out.println("❌ Target word пустое, статистика не записывается");
            return false;
        }
        
        GameStats gameStats = new GameStats(gameDate, attempts, localTimeService.getCurrentMoscowDateTime(), playerId,
                targetWord, gameTimeSeconds);
        
        GameStatsEntity entity = new GameStatsEntity(gameStats);
        gameStatsRepository.save(entity);
        
        System.out.println("✅ Статистика успешно записана в БД: " + gameStats);
        return true;
    }
    
    /**
     * Получает статистику дня из базы данных (по московскому времени)
     */
    public DailyStats getDailyStats(LocalDate date) {
        List<GameStatsEntity> dayStats = gameStatsRepository.findByGameDateOrderByAttemptsAscGameTimeSecondsAsc(date);
        
        if (dayStats.isEmpty()) {
            return new DailyStats(date, "", 0, 0, 0.0, new HashMap<>(), new ArrayList<>(), null);
        }
        
        // Получаем слово дня из первой записи
        String targetWord = dayStats.get(0).getTargetWord();
        
        // Подсчитываем общую статистику
        int totalPlayers = dayStats.size();
        int successfulPlayers = (int) dayStats.stream()
            .filter(GameStatsEntity::isSuccess)
            .count();
        
        double successRate = totalPlayers > 0 ? (double) successfulPlayers / totalPlayers * 100 : 0.0;
        
        // Распределение по попыткам
        Map<Integer, Integer> attemptsDistribution = dayStats.stream()
            .filter(GameStatsEntity::isSuccess)
            .collect(Collectors.groupingBy(
                GameStatsEntity::getAttempts,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        // Топ игроков (уже отсортированы в запросе)
        List<DailyStats.PlayerResult> topPlayers = dayStats.stream()
            .filter(GameStatsEntity::isSuccess)
            .map(stats -> new DailyStats.PlayerResult(
                stats.getPlayerId(),
                stats.getAttempts(),
                stats.getCompletedAt(),
                0, // Ранг будет установлен ниже
                true,
                stats.getGameTimeSeconds()
            ))
            .collect(Collectors.toList());
        
        // Устанавливаем ранги
        for (int i = 0; i < topPlayers.size(); i++) {
            topPlayers.get(i).setRank(i + 1);
        }
        
        return new DailyStats(
            date,
            targetWord,
            totalPlayers,
            successfulPlayers,
            successRate,
            attemptsDistribution,
            topPlayers,
            null
        );
    }
    
    /**
     * Получает статистику дня с результатом конкретного игрока
     */
    public DailyStats getDailyStatsWithPlayerResult(LocalDate date, String playerId) {
        DailyStats dailyStats = getDailyStats(date);
        
        // Находим результат конкретного игрока
        Optional<DailyStats.PlayerResult> playerResult = dailyStats.getTopPlayers().stream()
            .filter(result -> result.getPlayerId().equals(playerId))
            .findFirst();
        
        if (playerResult.isPresent()) {
            dailyStats.setPlayerResult(playerResult.get());
        } else {
            // Игрок не угадал или не играл
            dailyStats.setPlayerResult(new DailyStats.PlayerResult(
                playerId, 0, null, 0, false, 0
            ));
        }
        
        return dailyStats;
    }
    
    /**
     * Получает статистику за последние N дней
     */
    public List<DailyStats> getRecentStats(int days) {
        LocalDate endDate = localTimeService.getCurrentMoscowDate();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        List<GameStatsEntity> recentStats = gameStatsRepository.findByGameDateBetween(startDate, endDate);
        
        // Группируем по дням
        Map<LocalDate, List<GameStatsEntity>> statsByDate = recentStats.stream()
            .collect(Collectors.groupingBy(GameStatsEntity::getGameDate));
        
        List<DailyStats> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<GameStatsEntity> dayStats = statsByDate.getOrDefault(date, new ArrayList<>());
            result.add(calculateDailyStats(date, dayStats));
        }
        
        return result;
    }
    
    /**
     * Очищает старую статистику (старше указанного количества дней)
     */
    public void cleanupOldStats(int keepDays) {
        LocalDate cutoffDate = localTimeService.getCurrentMoscowDate().minusDays(keepDays);
        gameStatsRepository.deleteByGameDateBefore(cutoffDate);
    }
    
    /**
     * Принудительно обнуляет статистику для указанной даты
     */
    public void resetStatsForDate(LocalDate date) {
        gameStatsRepository.deleteByGameDate(date);
        System.out.println("🗑️ Статистика обнулена для даты: " + date);
    }
    
    /**
     * Получает информацию о текущем состоянии статистики
     */
    public String getStatsInfo() {
        StringBuilder info = new StringBuilder();
        info.append("📊 Информация о статистике:\n");
        
        long totalRecords = gameStatsRepository.count();
        if (totalRecords == 0) {
            info.append("   Статистика пуста\n");
        } else {
            info.append("   Всего записей в БД: ").append(totalRecords).append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * Вспомогательный метод для расчета статистики дня
     */
    private DailyStats calculateDailyStats(LocalDate date, List<GameStatsEntity> dayStats) {
        if (dayStats.isEmpty()) {
            return new DailyStats(date, "", 0, 0, 0.0, new HashMap<>(), new ArrayList<>(), null);
        }
        
        String targetWord = dayStats.get(0).getTargetWord();
        int totalPlayers = dayStats.size();
        int successfulPlayers = (int) dayStats.stream().filter(GameStatsEntity::isSuccess).count();
        double successRate = totalPlayers > 0 ? (double) successfulPlayers / totalPlayers * 100 : 0.0;
        
        Map<Integer, Integer> attemptsDistribution = dayStats.stream()
            .filter(GameStatsEntity::isSuccess)
            .collect(Collectors.groupingBy(
                GameStatsEntity::getAttempts,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        List<DailyStats.PlayerResult> topPlayers = dayStats.stream()
            .filter(GameStatsEntity::isSuccess)
            .map(stats -> new DailyStats.PlayerResult(
                stats.getPlayerId(),
                stats.getAttempts(),
                stats.getCompletedAt(),
                0,
                true,
                stats.getGameTimeSeconds()
            ))
            .collect(Collectors.toList());
        
        for (int i = 0; i < topPlayers.size(); i++) {
            topPlayers.get(i).setRank(i + 1);
        }
        
        return new DailyStats(date, targetWord, totalPlayers, successfulPlayers, successRate, 
                            attemptsDistribution, topPlayers, null);
    }
    
    /**
     * Проверяет, может ли игрок играть сегодня в режиме дня
     */
    public boolean canPlayerPlayToday(String playerId) {
        return dailyGameValidationService.canPlayerPlayToday(playerId);
    }
    
    /**
     * Получает информацию о том, почему игрок не может играть
     */
    public String getPlayRestrictionReason(String playerId) {
        return dailyGameValidationService.getPlayRestrictionReason(playerId);
    }
    
    /**
     * Получает статистику игрока за сегодня
     */
    public String getTodayPlayerStats(String playerId) {
        return dailyGameValidationService.getTodayPlayerStats(playerId);
    }
    
    /**
     * Получает информацию о времени для игры
     */
    public String getTimeInfo() {
        return dailyGameValidationService.getTimeInfo();
    }
}
