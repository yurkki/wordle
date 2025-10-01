package org.example.wordle;

import org.example.wordle.service.*;
import org.example.wordle.repository.WordsRepository;
import org.example.wordle.repository.ExtendedWordsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DictionaryApiTest {

    @Test
    public void testDictionaryApiService() {
        ExtendedWordsRepository extendedRepo = new ExtendedWordsRepository();
        DictionaryApiService apiService = new DictionaryApiService(extendedRepo);
        
        // Тестируем формат слов
        assertFalse(apiService.isWordValid("12345"), "12345 не должно быть валидным");
        assertFalse(apiService.isWordValid("HELLO"), "HELLO не должно быть валидным");
        assertFalse(apiService.isWordValid("ПОКЕ"), "ПОКЕ не должно быть валидным");
        assertFalse(apiService.isWordValid(null), "null не должно быть валидным");
        
        // Тестируем слова из расширенного словаря
        assertTrue(apiService.isWordValid("ЕПАРХ"), "ЕПАРХ должно быть валидным из расширенного словаря");
        assertTrue(apiService.isWordValid("ШАЛОМ"), "ШАЛОМ должно быть валидным из расширенного словаря");
        assertTrue(apiService.isWordValid("СУФЛЕ"), "СУФЛЕ должно быть валидным из расширенного словаря");
        
        // Проверяем статус API
        String status = apiService.getApiStatus();
        assertNotNull(status, "Статус API не должен быть null");
        assertTrue(status.contains("Расширенный словарь"), "Статус должен содержать информацию о расширенном словаре");
        assertTrue(status.contains("слов"), "Статус должен содержать количество слов");
        
        System.out.println("API Статус: " + status);
    }
    
    @Test
    public void testWordleServiceWithApi() {
        WordsRepository wordsRepository = new WordsRepository();
        ExtendedWordsRepository extendedRepo = new ExtendedWordsRepository();
        DictionaryApiService apiService = new DictionaryApiService(extendedRepo);
        PlayerIdService playerIdService = new PlayerIdService();
        LocalTimeService localTimeService = new LocalTimeService();
        DailyWordService dailyWordService = new DailyWordService(wordsRepository, apiService, localTimeService);
        StatsService statsService = new StatsService();
        WordleService wordleService = new WordleService(wordsRepository, dailyWordService, apiService, statsService, playerIdService, localTimeService);
        
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
