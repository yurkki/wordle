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

    public DailyWordService(WordsRepository wordsRepository) {
        this.wordsRepository = wordsRepository;
    }
    
    /**
     * Получить слово дня для указанной даты
     * 
     * Алгоритм использует детерминированный seed на основе даты для обеспечения
     * стабильности в течение дня, но добавляет рандомность для более равномерного
     * распределения слов по датам.
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
        
        // Добавляем дополнительную рандомность - делаем несколько случайных выборов
        // и берем последний для более равномерного распределения
        int wordIndex = 0;
        for (int i = 0; i < 3; i++) {
            wordIndex = random.nextInt(fiveLetterWords.size());
        }
        
        return fiveLetterWords.get(wordIndex);
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
