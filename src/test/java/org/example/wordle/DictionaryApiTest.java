package org.example.wordle;

import org.example.wordle.service.DictionaryApiService;
import org.example.wordle.service.WordleService;
import org.example.wordle.repository.WordsRepository;
import org.example.wordle.service.DailyWordService;
import org.example.wordle.service.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DictionaryApiTest {

    @Test
    public void testDictionaryApiService() {
        DictionaryApiService apiService = new DictionaryApiService();
        
        // Тестируем формат слов
        assertFalse(apiService.isWordValid("12345"), "12345 не должно быть валидным");
        assertFalse(apiService.isWordValid("HELLO"), "HELLO не должно быть валидным");
        assertFalse(apiService.isWordValid("ПОКЕ"), "ПОКЕ не должно быть валидным");
        assertFalse(apiService.isWordValid(null), "null не должно быть валидным");
        
        // Проверяем статус API
        String status = apiService.getApiStatus();
        assertNotNull(status, "Статус API не должен быть null");
        assertTrue(status.contains("Яндекс"), "Статус должен содержать информацию о Яндекс API");
        
        System.out.println("API Статус: " + status);
    }
    
    @Test
    public void testWordleServiceWithApi() {
        WordsRepository wordsRepository = new WordsRepository();
        DictionaryApiService apiService = new DictionaryApiService();
        DailyWordService dailyWordService = new DailyWordService(wordsRepository, apiService);
        StatsService statsService = new StatsService();
        WordleService wordleService = new WordleService(wordsRepository, dailyWordService, apiService, statsService);
        
        // Тестируем формат слов
        assertFalse(wordleService.isValidWord("12345"), "12345 не должно быть валидным");
        assertFalse(wordleService.isValidWord("HELLO"), "HELLO не должно быть валидным");
        assertFalse(wordleService.isValidWord("ПОКЕ"), "ПОКЕ не должно быть валидным");
        
        // Проверяем статистику
        String stats = wordleService.getDictionaryStats();
        assertNotNull(stats, "Статистика не должна быть null");
        assertTrue(stats.contains("Яндекс"), "Статистика должна содержать информацию о Яндекс API");
        
        System.out.println("Статистика: " + stats);
    }
}
