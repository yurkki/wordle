package org.example.wordle.service;

import org.example.wordle.repository.WordsRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
     */
    public String getWordForDate(LocalDate date) {
        // Используем хеш от даты для получения индекса слова
        int dayOfYear = date.getDayOfYear();
        
        // Получаем уже отфильтрованные 5-буквенные слова
        List<String> fiveLetterWords = wordsRepository.getFiveLetterWords();
        
        if (fiveLetterWords.isEmpty()) {
            throw new IllegalStateException("Нет доступных 5-буквенных слов");
        }
        
        // Используем индекс для выбора из 5-буквенных слов
        int wordIndex = dayOfYear % fiveLetterWords.size();
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
