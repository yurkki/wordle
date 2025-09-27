package org.example.wordle;

import org.example.wordle.service.FriendGameService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тест для функциональности игры с друзьями
 */
@SpringBootTest
public class FriendGameTest {

    @Test
    public void testSaveAndGetFriendWord() {
        FriendGameService friendGameService = new FriendGameService();
        
        // Сохраняем слово
        String word = "СЛОВО";
        String wordId = friendGameService.saveFriendWord(word);
        
        assertNotNull(wordId, "word_id не должен быть null");
        assertTrue(wordId.length() > 0, "word_id должен содержать символы");
        
        // Получаем слово по ID
        String retrievedWord = friendGameService.getFriendWord(wordId);
        assertEquals(word, retrievedWord, "Полученное слово должно совпадать с сохраненным");
        
        // Проверяем, что игра существует
        assertTrue(friendGameService.isFriendGameExists(wordId), "Игра должна существовать");
        
        // Проверяем время создания
        LocalDateTime timestamp = friendGameService.getGameTimestamp(wordId);
        assertNotNull(timestamp, "Время создания не должно быть null");
        
        System.out.println("✅ Тест сохранения и получения слова пройден");
        System.out.println("📝 Слово: " + word);
        System.out.println("🆔 ID: " + wordId);
        System.out.println("⏰ Время создания: " + timestamp);
    }
    
    @Test
    public void testInvalidWord() {
        FriendGameService friendGameService = new FriendGameService();
        
        // Тестируем слова неправильной длины
        assertThrows(IllegalArgumentException.class, () -> {
            friendGameService.saveFriendWord("СЛО"); // 3 буквы
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            friendGameService.saveFriendWord("СЛОВАРЬ"); // 7 букв
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            friendGameService.saveFriendWord(null); // null
        });
        
        System.out.println("✅ Тест валидации слов пройден");
    }
    
    @Test
    public void testNonExistentWord() {
        FriendGameService friendGameService = new FriendGameService();
        
        // Пытаемся получить несуществующее слово
        String nonExistentId = "nonexistent123";
        String word = friendGameService.getFriendWord(nonExistentId);
        assertNull(word, "Несуществующее слово должно возвращать null");
        
        // Проверяем, что игра не существует
        assertFalse(friendGameService.isFriendGameExists(nonExistentId), "Несуществующая игра должна возвращать false");
        
        System.out.println("✅ Тест несуществующих слов пройден");
    }
    
    @Test
    public void testMultipleWords() {
        FriendGameService friendGameService = new FriendGameService();
        
        // Сохраняем несколько слов
        String word1 = "СЛОВО";
        String word2 = "ИГРА";
        String word3 = "ТЕСТ";
        
        String id1 = friendGameService.saveFriendWord(word1);
        String id2 = friendGameService.saveFriendWord(word2);
        String id3 = friendGameService.saveFriendWord(word3);
        
        // Проверяем, что все ID уникальны
        assertNotEquals(id1, id2, "ID должны быть уникальными");
        assertNotEquals(id2, id3, "ID должны быть уникальными");
        assertNotEquals(id1, id3, "ID должны быть уникальными");
        
        // Проверяем, что все слова сохранились
        assertEquals(word1, friendGameService.getFriendWord(id1));
        assertEquals(word2, friendGameService.getFriendWord(id2));
        assertEquals(word3, friendGameService.getFriendWord(id3));
        
        System.out.println("✅ Тест множественных слов пройден");
        System.out.println("📝 Слово 1: " + word1 + " (ID: " + id1 + ")");
        System.out.println("📝 Слово 2: " + word2 + " (ID: " + id2 + ")");
        System.out.println("📝 Слово 3: " + word3 + " (ID: " + id3 + ")");
    }
    
    @Test
    public void testStats() {
        FriendGameService friendGameService = new FriendGameService();
        
        // Изначально статистика должна быть пустой
        String initialStats = friendGameService.getFriendGamesStats();
        assertTrue(initialStats.contains("Всего игр с друзьями: 0"), "Изначально должно быть 0 игр");
        
        // Добавляем несколько игр
        friendGameService.saveFriendWord("СЛОВО");
        friendGameService.saveFriendWord("ИГРА");
        
        String statsAfter = friendGameService.getFriendGamesStats();
        assertTrue(statsAfter.contains("Всего игр с друзьями: 2"), "После добавления должно быть 2 игры");
        
        System.out.println("✅ Тест статистики пройден");
        System.out.println("📊 Начальная статистика: " + initialStats);
        System.out.println("📊 Статистика после добавления: " + statsAfter);
    }
}
