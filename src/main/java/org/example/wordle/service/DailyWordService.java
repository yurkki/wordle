package org.example.wordle.service;

import org.example.wordle.repository.WordsRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * Сервис для управления ежедневными словами
 */
@Service
public class DailyWordService {

    private final WordsRepository wordsRepository;
    private final DictionaryApiService dictionaryApiService;

    public DailyWordService(WordsRepository wordsRepository, DictionaryApiService dictionaryApiService) {
        this.wordsRepository = wordsRepository;
        this.dictionaryApiService = dictionaryApiService;
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
     * Получить слово дня для сегодняшней даты
     */
    public String getTodayWord() {
        return getWordForDate(LocalDate.now());
    }
    
    /**
     * Проверить, является ли слово словом дня
     */
    public boolean isTodayWord(String word) {
        return word != null && word.equalsIgnoreCase(getTodayWord());
    }
    
    /**
     * Получить дату в формате строки для отображения
     */
    public String getTodayDateString() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
    }
}
