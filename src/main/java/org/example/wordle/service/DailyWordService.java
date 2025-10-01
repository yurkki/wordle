package org.example.wordle.service;

import org.example.wordle.repository.WordsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * Сервис для управления ежедневными словами
 */
@Service
public class DailyWordService {

    private final WordsRepository wordsRepository;
    private final DictionaryApiService dictionaryApiService;
    private final LocalTimeService localTimeService;
    
    // Кэш для слова дня - избегаем повторного выбора в течение дня
    private String cachedTodayWord = null;
    private LocalDate cachedDate = null;

    @Autowired
    public DailyWordService(WordsRepository wordsRepository, DictionaryApiService dictionaryApiService, LocalTimeService localTimeService) {
        this.wordsRepository = wordsRepository;
        this.dictionaryApiService = dictionaryApiService;
        this.localTimeService = localTimeService;
    }
    
    /**
     * Получить слово дня для указанной даты
     * 
     * Алгоритм использует детерминированный seed на основе даты для обеспечения
     * стабильности в течение дня, но добавляет рандомность для более равномерного
     * распределения слов по датам. Включает валидацию через Яндекс API.
     */
    public String getWordForDate(LocalDate date) {
        // Получаем уже отфильтрованные 5-буквенные слова
        List<String> fiveLetterWords = wordsRepository.getFiveLetterWords();
        
        if (fiveLetterWords.isEmpty()) {
            throw new IllegalStateException("Нет доступных 5-буквенных слов");
        }
        
        // Создаем детерминированный seed на основе даты для стабильности в течение дня
        // Используем более сложную формулу для лучшего распределения
        long year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        
        // Создаем seed с использованием простых чисел для лучшего распределения
        long seed = year * 10000L + month * 100L + day;
        seed = seed * 31L + 17L; // Добавляем простые числа для лучшего распределения
        
        // Создаем Random с детерминированным seed
        Random random = new Random(seed);
        
        // Ищем валидное слово с проверкой через Яндекс API
        return findValidWordWithApiCheck(fiveLetterWords, random, date);
    }
    
    /**
     * Ищет валидное слово с проверкой через Яндекс API
     * Если API недоступен или слово не проходит валидацию, выбирает другое слово
     */
    private String findValidWordWithApiCheck(List<String> words, Random random, LocalDate date) {
        int maxAttempts = Math.min(50, words.size()); // Ограничиваем количество попыток
        int baseIndex = 0;
        
        // Добавляем дополнительную рандомность - делаем несколько случайных выборов
        // и берем последний для более равномерного распределения
        for (int i = 0; i < 3; i++) {
            baseIndex = random.nextInt(words.size());
        }
        
        // Пробуем найти валидное слово, начиная с базового индекса
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int wordIndex = (baseIndex + attempt) % words.size();
            String candidateWord = words.get(wordIndex);
            
            System.out.println("Проверяем слово дня для " + date + ": " + candidateWord + " (попытка " + (attempt + 1) + ")");
            
            // Проверяем слово через Яндекс API
            if (dictionaryApiService.isWordValid(candidateWord)) {
                System.out.println("Слово дня выбрано: " + candidateWord + " (валидация через Яндекс API пройдена)");
                return candidateWord;
            } else {
                System.out.println("Слово " + candidateWord + " не прошло валидацию через Яндекс API, пробуем следующее");
            }
        }
        
        // Если не удалось найти валидное слово через API, возвращаем базовое слово
        String fallbackWord = words.get(baseIndex);
        System.out.println("Не удалось найти валидное слово через API, используем fallback: " + fallbackWord);
        return fallbackWord;
    }
    
    /**
     * Получить слово дня для сегодняшней даты (по московскому времени)
     * Использует кэширование для избежания повторного выбора в течение дня
     */
    public String getTodayWord() {
        LocalDate today = localTimeService.getCurrentMoscowDate();
        
        // Проверяем кэш - если слово уже выбрано сегодня, возвращаем его
        if (cachedTodayWord != null && cachedDate != null && cachedDate.equals(today)) {
            System.out.println("📝 Используем кэшированное слово дня: " + cachedTodayWord + " (дата: " + today + ")");
            return cachedTodayWord;
        }
        
        // Выбираем новое слово дня
        System.out.println("🎯 Выбираем новое слово дня для " + today + " (московское время)");
        String todayWord = getWordForDate(today);
        
        // Сохраняем в кэш
        cachedTodayWord = todayWord;
        cachedDate = today;
        
        System.out.println("✅ Слово дня выбрано и закэшировано: " + todayWord + " (дата: " + today + ")");
        return todayWord;
    }
    
    /**
     * Проверить, является ли слово словом дня
     */
    public boolean isTodayWord(String word) {
        return word != null && word.equalsIgnoreCase(getTodayWord());
    }
    
    /**
     * Получить дату в формате строки для отображения (по московскому времени)
     */
    public String getTodayDateString() {
        return localTimeService.getCurrentMoscowDateString();
    }
    
    /**
     * Принудительно обновить кэш слова дня
     * Полезно для тестирования или принудительного обновления
     */
    public void clearCache() {
        cachedTodayWord = null;
        cachedDate = null;
        System.out.println("🗑️ Кэш слова дня очищен");
    }
    
    /**
     * Получить информацию о текущем кэше
     */
    public String getCacheInfo() {
        if (cachedTodayWord != null && cachedDate != null) {
            return "Кэш: " + cachedTodayWord + " (дата: " + cachedDate + ")";
        } else {
            return "Кэш пуст";
        }
    }
}
