package org.example.wordle.service;

import org.example.wordle.model.DailyStats;
import org.example.wordle.model.GameStats;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Сервис для сбора и анализа статистики игр
 */
@Service
public class StatsService {
    
    // Хранилище статистики по дням
    // Ключ: дата игры, Значение: список статистики игроков
    private final Map<LocalDate, List<GameStats>> dailyStats = new ConcurrentHashMap<>();
    
    // Кэш для отслеживания, была ли уже выполнена очистка за текущий день
    private LocalDate lastCleanupDate = null;
    
    /**
     * Записывает статистику завершенной игры
     */
    public void recordGameStats(LocalDate gameDate, int attempts, String playerId, String targetWord, int gameTimeSeconds) {
        // Проверяем, нужно ли обнулить статистику при смене дня
        ensureCurrentDayStats(gameDate);
        
        GameStats stats = new GameStats(
            gameDate, 
            attempts, 
            LocalDateTime.now(), 
            playerId, 
            targetWord,
            gameTimeSeconds
        );
        
        dailyStats.computeIfAbsent(gameDate, k -> new ArrayList<>()).add(stats);
        
        System.out.println("📊 Записана статистика игры: " + stats);
        System.out.println("📈 Общая статистика за " + gameDate + ": " + dailyStats.get(gameDate).size() + " игроков");
    }
    
    /**
     * Получает статистику дня
     */
    public DailyStats getDailyStats(LocalDate date) {
        // Проверяем, нужно ли обнулить статистику при смене дня
        ensureCurrentDayStats(date);
        
        List<GameStats> dayStats = dailyStats.getOrDefault(date, new ArrayList<>());
        
        if (dayStats.isEmpty()) {
            return new DailyStats(date, "", 0, 0, 0.0, new HashMap<>(), new ArrayList<>(), null);
        }
        
        // Получаем слово дня из первой записи
        String targetWord = dayStats.get(0).getTargetWord();
        
        // Подсчитываем общую статистику
        int totalPlayers = dayStats.size();
        int successfulPlayers = (int) dayStats.stream()
            .filter(GameStats::isSuccess)
            .count();
        
        double successRate = totalPlayers > 0 ? (double) successfulPlayers / totalPlayers * 100 : 0.0;
        
        // Распределение по попыткам
        Map<Integer, Integer> attemptsDistribution = dayStats.stream()
            .filter(GameStats::isSuccess)
            .collect(Collectors.groupingBy(
                GameStats::getAttempts,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        // Топ игроков (сортировка по попыткам, затем по времени игры)
        List<DailyStats.PlayerResult> topPlayers = dayStats.stream()
            .filter(GameStats::isSuccess)
            .sorted((a, b) -> {
                // Сначала по количеству попыток (меньше = лучше)
                int attemptsCompare = Integer.compare(a.getAttempts(), b.getAttempts());
                if (attemptsCompare != 0) {
                    return attemptsCompare;
                }
                // Затем по времени игры (меньше = лучше)
                return Integer.compare(a.getGameTimeSeconds(), b.getGameTimeSeconds());
            })
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
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        List<DailyStats> recentStats = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            recentStats.add(getDailyStats(date));
        }
        
        return recentStats;
    }
    
    /**
     * Очищает старую статистику (старше указанного количества дней)
     */
    public void cleanupOldStats(int keepDays) {
        LocalDate cutoffDate = LocalDate.now().minusDays(keepDays);
        dailyStats.entrySet().removeIf(entry -> entry.getKey().isBefore(cutoffDate));
    }
    
    /**
     * Проверяет и обеспечивает корректную статистику для текущего дня
     * Автоматически обнуляет статистику при смене дня
     * Использует кэширование для оптимизации производительности
     */
    private void ensureCurrentDayStats(LocalDate currentDate) {
        LocalDate today = LocalDate.now();
        
        // Если запрашивается статистика не за сегодня, ничего не делаем
        if (!currentDate.equals(today)) {
            return;
        }
        
        // Проверяем кэш - если очистка уже была выполнена сегодня, пропускаем
        if (lastCleanupDate != null && lastCleanupDate.equals(today)) {
            return; // Очистка уже была выполнена сегодня
        }
        
        // Проверяем, есть ли статистика за предыдущие дни
        boolean hasOldStats = dailyStats.keySet().stream()
            .anyMatch(date -> date.isBefore(today));
        
        if (hasOldStats) {
            System.out.println("🔄 Обнаружена статистика за предыдущие дни, очищаем старые данные...");
            
            // Очищаем статистику за предыдущие дни, оставляя только сегодняшнюю
            dailyStats.entrySet().removeIf(entry -> entry.getKey().isBefore(today));
            
            System.out.println("✅ Статистика обнулена для нового дня: " + today);
            System.out.println("📊 Текущая статистика за " + today + ": " + 
                dailyStats.getOrDefault(today, new ArrayList<>()).size() + " игроков");
        }
        
        // Обновляем кэш - отмечаем, что очистка была выполнена сегодня
        lastCleanupDate = today;
    }
    
    /**
     * Принудительно обнуляет статистику для указанной даты
     */
    public void resetStatsForDate(LocalDate date) {
        dailyStats.remove(date);
        System.out.println("🗑️ Статистика обнулена для даты: " + date);
    }
    
    /**
     * Получает информацию о текущем состоянии статистики
     */
    public String getStatsInfo() {
        StringBuilder info = new StringBuilder();
        info.append("📊 Информация о статистике:\n");
        
        if (dailyStats.isEmpty()) {
            info.append("   Статистика пуста\n");
        } else {
            dailyStats.forEach((date, stats) -> {
                info.append("   ").append(date).append(": ").append(stats.size()).append(" игроков\n");
            });
        }
        
        return info.toString();
    }
}
