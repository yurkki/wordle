package org.example.wordle.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Сервис для управления играми с друзьями
 */
@Service
public class FriendGameService {

    @Autowired
    private LocalTimeService localTimeService;

    // Хранилище игр с друзьями: word_id -> загаданное слово
    private final Map<String, String> friendGames = new ConcurrentHashMap<>();
    
    // Хранилище метаданных: word_id -> время создания
    private final Map<String, LocalDateTime> gameTimestamps = new ConcurrentHashMap<>();

    /**
     * Сохранить слово для игры с другом
     * @param word загаданное слово
     * @return word_id для ссылки
     */
    public String saveFriendWord(String word) {
        if (word == null || word.length() != 5) {
            throw new IllegalArgumentException("Слово должно содержать ровно 5 букв");
        }
        
        // Создаем уникальный ID для слова
        String wordId = generateWordId(word);
        
        // Сохраняем слово и метаданные
        friendGames.put(wordId, word.toUpperCase());
        gameTimestamps.put(wordId, localTimeService.getCurrentMoscowDateTime());
        
        System.out.println("🎯 Сохранено слово для игры с другом: " + word.toUpperCase() + " (ID: " + wordId + ")");
        
        return wordId;
    }
    
    /**
     * Получить слово по word_id
     * @param wordId идентификатор слова
     * @return загаданное слово или null если не найдено
     */
    public String getFriendWord(String wordId) {
        if (wordId == null || wordId.isEmpty()) {
            return null;
        }
        
        String word = friendGames.get(wordId);
        if (word != null) {
            System.out.println("🔍 Найдено слово для игры с другом: " + word + " (ID: " + wordId + ")");
        } else {
            System.out.println("❌ Слово с ID " + wordId + " не найдено");
        }
        
        return word;
    }
    
    /**
     * Проверить, существует ли игра с данным word_id
     * @param wordId идентификатор слова
     * @return true если игра существует
     */
    public boolean isFriendGameExists(String wordId) {
        return wordId != null && friendGames.containsKey(wordId);
    }
    
    /**
     * Получить время создания игры
     * @param wordId идентификатор слова
     * @return время создания или null
     */
    public LocalDateTime getGameTimestamp(String wordId) {
        return gameTimestamps.get(wordId);
    }
    
    /**
     * Генерирует уникальный ID для слова
     * Использует комбинацию UUID и кодирования слова
     */
    private String generateWordId(String word) {
        // Создаем UUID для уникальности
        String uuid = UUID.randomUUID().toString().replace("-", "");
        
        // Кодируем слово в Base64 для дополнительной безопасности
        String encodedWord = Base64.getEncoder().encodeToString(word.getBytes());
        
        // Берем первые 8 символов UUID + первые 8 символов закодированного слова
        String shortUuid = uuid.substring(0, 8);
        String shortEncoded = encodedWord.substring(0, 8);
        
        // Объединяем и возвращаем
        return shortUuid + shortEncoded;
    }
    
    /**
     * Получить статистику игр с друзьями
     */
    public String getFriendGamesStats() {
        int totalGames = friendGames.size();
        return "Всего игр с друзьями: " + totalGames;
    }
    
    /**
     * Очистить старые игры (старше 7 дней)
     * Вызывается периодически для очистки
     */
    public void cleanupOldGames() {
        LocalDateTime cutoff = localTimeService.getCurrentMoscowDateTime().minusDays(7);
        int removedCount = 0;
        
        gameTimestamps.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(cutoff)) {
                friendGames.remove(entry.getKey());
                return true;
            }
            return false;
        });
        
        System.out.println("🧹 Очищено старых игр с друзьями: " + removedCount);
    }
}
