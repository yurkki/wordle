package org.example.wordle.repository;

import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Репозиторий для расширенного словаря слов
 * Содержит слова, которых может не быть в Яндекс словаре, но которые являются валидными русскими словами
 */
@Repository
public class ExtendedWordsRepository {

    @Getter
    private final List<String> extendedWords;
    private final Set<String> validExtendedWords;

    public ExtendedWordsRepository() {
        this.extendedWords = Arrays.asList(
                "ЕПАРХ", "ШАЛОМ", "СУФЛЕ", "ТУНЕЦ", "МИЛАН", "ТАСКА", "МИКСТ", "АНАПА", "ВОПЕЖ", "ВОПЁЖ", "КАРМА", "КЕГЛЯ",
                "ПОРНО", "РАПИД", "САМСА", "КАРАТ", "УРОКИ", "ГРУША", "ПАНДА", "ВАНГА", "ГЛЯСЕ", "СКВАД"
        );

        // Создаем Set для быстрой проверки валидности слов (нормализованные)
        this.validExtendedWords = extendedWords.stream()
                .map(this::normalizeWord)
                .collect(Collectors.toSet());
    }

    /**
     * Проверить, есть ли слово в расширенном словаре
     */
    public boolean containsWord(String word) {
        return word != null && validExtendedWords.contains(normalizeWord(word));
    }
    
    /**
     * Нормализует слово, заменяя ё на е для унификации
     */
    private String normalizeWord(String word) {
        return word.toUpperCase()
                .replace('Ё', 'Е')
                .replace('ё', 'Е');
    }
    
    /**
     * Получить все слова из расширенного словаря
     */
    public List<String> getAllWords() {
        return extendedWords;
    }
    
    /**
     * Получить количество слов в расширенном словаре
     */
    public int getWordCount() {
        return extendedWords.size();
    }
}
