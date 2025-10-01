package org.example.wordle.service;

import org.example.wordle.model.GameStatsEntity;
import org.example.wordle.repository.GameStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Сервис для валидации игр в режиме дня
 * Обеспечивает, что каждый игрок может играть только один раз в день
 */
@Service
public class DailyGameValidationService {
    
    @Autowired
    private GameStatsRepository gameStatsRepository;
    
    @Autowired
    private LocalTimeService localTimeService;
    
    /**
     * Проверяет, может ли игрок играть в режиме дня сегодня
     * Возвращает true, если игрок еще не играл сегодня
     */
    public boolean canPlayerPlayToday(String playerId) {
        LocalDate today = localTimeService.getCurrentMoscowDate();
        
        // Ищем записи игрока за сегодня
        GameStatsEntity existingGame = gameStatsRepository.findByGameDateAndPlayerId(today, playerId);
        
        boolean canPlay = existingGame == null;
        
        if (canPlay) {
            System.out.println("✅ Игрок " + playerId + " может играть сегодня (" + today + ")");
        } else {
            System.out.println("❌ Игрок " + playerId + " уже играл сегодня (" + today + "), результат: " + 
                (existingGame.isSuccess() ? existingGame.getAttempts() + " попыток" : "не угадал"));
        }
        
        return canPlay;
    }
    
    /**
     * Проверяет, играл ли игрок сегодня в режиме дня
     */
    public boolean hasPlayerPlayedToday(String playerId) {
        return !canPlayerPlayToday(playerId);
    }
    
    /**
     * Получает результат игры игрока за сегодня (если играл)
     */
    public GameStatsEntity getTodayGameResult(String playerId) {
        LocalDate today = localTimeService.getCurrentMoscowDate();
        return gameStatsRepository.findByGameDateAndPlayerId(today, playerId);
    }
    
    /**
     * Проверяет, может ли игрок играть в режиме дня в указанную дату
     */
    public boolean canPlayerPlayOnDate(String playerId, LocalDate date) {
        GameStatsEntity existingGame = gameStatsRepository.findByGameDateAndPlayerId(date, playerId);
        return existingGame == null;
    }
    
    /**
     * Получает информацию о том, почему игрок не может играть
     */
    public String getPlayRestrictionReason(String playerId) {
        LocalDate today = localTimeService.getCurrentMoscowDate();
        GameStatsEntity existingGame = gameStatsRepository.findByGameDateAndPlayerId(today, playerId);
        
        if (existingGame == null) {
            return "Игрок может играть";
        }
        
        if (existingGame.isSuccess()) {
            return String.format("Игрок уже угадал слово за %d попыток в %s", 
                existingGame.getAttempts(),
                existingGame.getCompletedAt().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        } else {
            return String.format("Игрок уже играл сегодня в %s, но не угадал слово", 
                existingGame.getCompletedAt().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        }
    }
    
    /**
     * Получает статистику игрока за сегодня
     */
    public String getTodayPlayerStats(String playerId) {
        GameStatsEntity game = getTodayGameResult(playerId);
        
        if (game == null) {
            return "Игрок еще не играл сегодня";
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("📊 Статистика за сегодня:\n");
        stats.append("   Слово дня: ").append(game.getTargetWord()).append("\n");
        stats.append("   Результат: ");
        
        if (game.isSuccess()) {
            stats.append("✅ Угадал за ").append(game.getAttempts()).append(" попыток");
        } else {
            stats.append("❌ Не угадал");
        }
        
        stats.append("\n   Время игры: ").append(game.getGameTimeSeconds()).append(" сек");
        stats.append("\n   Завершено в: ").append(
            game.getCompletedAt().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        );
        
        return stats.toString();
    }
    
    /**
     * Проверяет, является ли время валидным для игры в режиме дня
     * Игра возможна с 00:01 до 23:59 по московскому времени
     */
    public boolean isValidTimeForDailyGame() {
        LocalDateTime now = localTimeService.getCurrentMoscowDateTime();
        LocalDate today = localTimeService.getCurrentMoscowDate();
        
        // Проверяем, что время в пределах дня (00:00:01 - 23:59:59)
        LocalDateTime startOfDay = today.atTime(0, 0, 1); // 00:00:01
        LocalDateTime endOfDay = today.atTime(23, 59, 59); // 23:59:59
        
        boolean isValid = !now.isBefore(startOfDay) && !now.isAfter(endOfDay);
        
        if (!isValid) {
            System.out.println("❌ Время не подходит для игры в режиме дня: " + 
                now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
        
        return isValid;
    }
    
    /**
     * Получает информацию о времени для игры
     */
    public String getTimeInfo() {
        LocalDateTime now = localTimeService.getCurrentMoscowDateTime();
        LocalDate today = localTimeService.getCurrentMoscowDate();
        
        StringBuilder info = new StringBuilder();
        info.append("🕐 Время игры:\n");
        info.append("   Текущее время: ").append(
            now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        ).append("\n");
        info.append("   Дата: ").append(today).append("\n");
        info.append("   Можно играть: ").append(isValidTimeForDailyGame() ? "✅ Да" : "❌ Нет");
        
        return info.toString();
    }
}
