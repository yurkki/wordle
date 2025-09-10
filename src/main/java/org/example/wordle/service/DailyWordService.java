package org.example.wordle.service;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Сервис для управления ежедневными словами
 */
@Service
public class DailyWordService {
    
    // Список слов для каждого дня (только 5-буквенные слова)
    private final List<String> dailyWords = Arrays.asList(
        "УСПЕХ", "АВТОР", "БАГАЖ", "ВЕТКА", "ГЛАЗА", "ЖИЗНЬ", "ИГРОК",
        "КАРТА", "ЛЕСОК", "МАСКА", "НОЖКА", "ПАРТА", "РЫБАК", "ТАБЛО",
        "ФИЛЬМ", "ЦВЕТОК", "ШАПКА", "ЭКРАН", "АБЗАЦ",
        "АГЕНТ", "АДРЕС", "АЗАРТ", "АЗИАТ", "АКРЫЛ", "АКТИВ", "АЛМАЗ", "АЛЬБА", "АМБАР", "АМПЕР",
        "АНГАР", "АНИСА", "АПЕЛЬ", "АРБУЗ", "АРЕНА", "АРХИВ", "АСТРА", "АТЛАС", "БАЗАР", "БАЙКА",
        "БАЛЕТ", "БАНАН", "БАРЖА", "БАСНЯ", "БАТОН", "БЕГУН", "БЕЛКА", "БЕРЕГ", "БИЛЕТ", "БИРЖА",
        "БЛОКА", "БОГАТ", "БОЖИЙ", "БОКАЛ", "БОМБА", "БОРОН", "ВАГОН", "ВАЗОН", "ВАЛЕТ", "ВАЛЬС",
        "ВАННА", "ВЕСНА", "ВИДЕО", "ВИЛКА", "ВИНТА", "ВОЛНА", "ВОРОТ", "ВОСТОК", "ВЫБОР"
    );
    
    /**
     * Получить слово дня для указанной даты
     */
    public String getWordForDate(LocalDate date) {
        // Используем хеш от даты для получения индекса слова
        int dayOfYear = date.getDayOfYear();
        int wordIndex = dayOfYear % dailyWords.size();
        return dailyWords.get(wordIndex);
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
