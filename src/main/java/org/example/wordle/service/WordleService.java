package org.example.wordle.service;

import org.example.wordle.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Сервис для логики игры Wordle
 */
@Service
public class WordleService {

    private final List<String> wordList;
    private final Set<String> validWords;

    private final DailyWordService dailyWordService;

    public WordleService(DailyWordService dailyWordService) {
        this.dailyWordService = dailyWordService;

        // Список русских слов из 5 букв
        this.wordList = Arrays.asList(
                "АБЗАЦ", "АВТОР", "АГЕНТ", "АДРЕС", "АЗАРТ", "АЗИАТ", "АКРЫЛ", "АКТИВ", "АЛМАЗ", "АЛЬБА",
                "АМБАР", "АМПЕР", "АНГАР", "АНИСА", "АПЕЛЬ", "АРБУЗ", "АРЕНА", "АРХИВ", "АСТРА", "АТЛАС",
                "БАГАЖ", "БАЗАР", "БАЙКА", "БАЛЕТ", "БАНАН", "БАРЖА", "БАСНЯ", "БАТОН", "БЕГУН", "БЕЛКА",
                "БЕРЕГ", "БИЛЕТ", "БИРЖА", "БЛОКА", "БОГАТ", "БОЖИЙ", "БОКАЛ", "БОМБА", "БОРОН",
                "ВАГОН", "ВАЗОН", "ВАЛЕТ", "ВАЛЬС", "ВАННА", "ВЕСНА", "ВЕТКА", "ВИДЕО", "ВИЛКА", "ВИНТА",
                "ВОЛНА", "ВОРОТ", "ВЫБОР", "ВЫСОТ", "ГАЗОН", "ГАММА", "ГАРАЖ", "ГВОЗД",
                "ГЕРОЙ", "ГИБКА", "ГЛАЗА", "ГОЛОС", "ГОРКА", "ГРУША", "ГУСАК", "ДАВКА",
                "ДЕБЕТ", "ДЕВКА", "ДЕТКА", "ДИВАН", "ДОБРО",
                "ДОСКА", "ДОЧКА", "ДУБОК", "ЕПАРХ", "ЕРЕСЬ", "ЖАТВА", "ЖИВОТ",
                "ЗАБОР", "ЗАВОД", "ЗАГАР", "ЗАДОК", "ЗАКАЗ", "ЗАМОК", "ЗАПАД", "ЗЕБРА", "ЗЕМЛЯ",
                "ИДЕАЛ", "ИКОНА", "ИОНИЙ", "ИСТОК", "КАБАН", "КАЗАК", "КАНАЛ", "КАПЛЯ", "КАРТА", "КАТОК",
                "КЛАСС", "КНИГА", "КОЛОС", "КОНЕЦ", "КОПИЯ", "КРЕСТ", "КРОВЬ",
                "КУКЛА", "ЛАВКА", "ЛАМПА", "ЛИМОН", "ЛОЖКА", "МАГИЯ", "МАСЛО", "МЕСТО", "МЕТРО",
                "НАРОД", "НЕДРА", "НЕФТЬ", "НУЖДА", "ОГОНЬ", "ОЗЕРО", "УСПЕХ", "АВТОР", "БАГАЖ", "ВЕТКА", "ГЛАЗА", "ЖИЗНЬ", "ИГРОК",
                "УСПЕХ", "АВТОР", "БАГАЖ", "ВЕТКА", "ГЛАЗА", "ЖИЗНЬ", "ИГРОК",
                "КАРТА", "ЛЕСОК", "МАСКА", "НОЖКА", "ПАРТА", "РЫБАК", "ТАБЛО",
                "ФИЛЬМ", "ШАПКА", "ЭКРАН", "АБЗАЦ",
                "АГЕНТ", "АДРЕС", "АЗАРТ", "АЗИАТ", "АКРЫЛ", "АКТИВ", "АЛМАЗ", "АЛЬБА", "АМБАР", "АМПЕР",
                "АНГАР", "АНИСА", "АПЕЛЬ", "АРБУЗ", "АРЕНА", "АРХИВ", "АСТРА", "АТЛАС", "БАЗАР", "БАЙКА",
                "БАЛЕТ", "БАНАН", "БАРЖА", "БАСНЯ", "БАТОН", "БЕГУН", "БЕЛКА", "БЕРЕГ", "БИЛЕТ", "БИРЖА",
                "БОКАЛ", "БОМБА", "БОРОН", "ВАГОН", "ВАЗОН", "ВАЛЕТ", "ВАЛЬС",
                "ВАННА", "ВЕСНА", "ВИДЕО", "ВИЛКА", "ВОЛНА", "ВОРОТ", "ВЫБОР",
                "АКТЁР", "АМЁБА", "АФЁРА", "АКЦИЯ", "АЛЛЕЯ", "АРМИЯ",
                "БРАВО", "БРАГА", "БРАГИ", "БРАДА", "БРАМА", "БРАНЬ", "БРАСС",
                "БРЕЙК", "БРЕМЯ", "БРЕНД", "БРЕНЬ", "БРЕШЬ",
                "ВОПЁЖ", "ВОРЬЁ", "ВАФЛЯ",
                "ГАЯТЬ", "ГЕВЕЯ", "ГИЛЯК", "ГИНЕЯ", "ГЛАМЯ", "ГЛЯДИ", "ГЛЯДЬ", "ГЛЯЖУ", "ГЛЯСЕ", "ГЛЯЧЕ",
                "ГОЛЯК", "ДЕНЁК", "ДОМЁК", "ДВОЯК", "ЗАЛЁТ", "ЗЯТЁК", "ЗАВЕЯ", "ЗАРЯД", "ЗАТЕЯ", "ЗБРУЯ",
                "КЛЁШИ", "КОВЁР", "КОЗЁЛ", "КОНЁК", "КОПЬЁ", "КОТЁЛ", "КУЛЁК", "КУТЁЖ", "КАПЛЯ",
                "ЛАРЁК", "ЛИКЁР", "ЛИНЁК", "ЛИТЬЁ", "ЛАДЬЯ", "ЛАЗНЯ", "ЛЕПНЯ",
                "ЛИЛИЯ", "БИЛЕТ", "КНИГА", "ВЕСНА", "ОГОНЬ", "ДРОВА", "ПЕСОК", "СТЕПЬ", "ГОРОД", "ПТИЦА",
                "РУЧЕЙ", "КАМНИ", "ЛЫЖНЯ", "ЧАЙКА", "САХАР", "СЕДЛО", "ПОЕЗД", "ЛАМПА",
                "ДИВАН", "РУЧКА", "КИСТЬ", "ТАНЕЦ", "ЗАРЯД", "ШАПКА", "ОБУВЬ", "НИТКА", "ТКАНЬ",
                "ЖИЛЕТ", "НОСКИ", "САПОГ", "РАМКА", "ФИЛЬМ", "СЦЕНА", "АКТЁР", "РОЛИК", "ДУБЛЬ", "СЮЖЕТ",
                "МЫСЛЬ", "РИФМА", "БУКВА", "ШКОЛА", "КЛАСС", "НАУКА", "ХИМИЯ", "ЧИСЛО", "ПАПКА",
                "ТЕКСТ", "ШРИФТ", "СКРИП", "ДОМЕН", "ПОЧТА", "ДРАЙВ"
        );

        // Проверяем, что все слова имеют длину 5 букв
        for (String word : wordList) {
            if (word.length() != 5) {
                throw new IllegalArgumentException("Слово '" + word + "' имеет длину " + word.length() + " букв, ожидается 5");
            }
        }

        this.validWords = new HashSet<>(wordList);
    }

    /**
     * Создает новую игру с случайным словом в режиме угадывания
     */
    public GameState createNewGame() {
        Random random = new Random();
        String targetWord = wordList.get(random.nextInt(wordList.size()));
        return new GameState(targetWord, GameMode.GUESS);
    }

    /**
     * Создает новую игру в режиме слова дня
     */
    public GameState createDailyGame() {
        String targetWord = dailyWordService.getTodayWord();
        return new GameState(targetWord, GameMode.DAILY);
    }

    /**
     * Создает игру в указанном режиме
     */
    public GameState createGame(GameMode mode) {
        if (mode == GameMode.DAILY) {
            return createDailyGame();
        } else {
            return createNewGame();
        }
    }

    /**
     * Проверяет, является ли слово валидным (любое слово из 5 букв)
     */
    public boolean isValidWord(String word) {
        return word != null &&
               word.length() == 5 &&
               word.matches("[а-яА-Я]{5}");
    }

    /**
     * Обрабатывает попытку угадать слово
     */
    public WordGuess processGuess(String guess, GameState gameState) {
        if (!gameState.canMakeGuess()) {
            throw new IllegalStateException("Игра уже завершена");
        }

        String upperGuess = guess.toUpperCase();
        String targetWord = gameState.getTargetWord();

        WordGuess wordGuess = new WordGuess(upperGuess);

        // Сначала отмечаем все буквы как отсутствующие
        for (int i = 0; i < 5; i++) {
            wordGuess.getLetters().get(i).setState(LetterState.ABSENT);
        }

        // Создаем копию загаданного слова для отслеживания использованных букв
        char[] targetChars = targetWord.toCharArray();
        boolean[] usedTargetChars = new boolean[5];

        // Первый проход: отмечаем правильные позиции (зеленые)
        for (int i = 0; i < 5; i++) {
            char guessChar = upperGuess.charAt(i);
            if (guessChar == targetChars[i]) {
                wordGuess.getLetters().get(i).setState(LetterState.CORRECT);
                usedTargetChars[i] = true;
            }
        }

        // Второй проход: отмечаем буквы, которые есть в слове, но на неправильных позициях (желтые)
        for (int i = 0; i < 5; i++) {
            char guessChar = upperGuess.charAt(i);
            
            // Пропускаем уже правильно размещенные буквы
            if (wordGuess.getLetters().get(i).getState() == LetterState.CORRECT) {
                continue;
            }
            
            // Ищем эту букву в загаданном слове на неправильных позициях
            for (int j = 0; j < 5; j++) {
                if (!usedTargetChars[j] && targetChars[j] == guessChar) {
                    wordGuess.getLetters().get(i).setState(LetterState.PRESENT);
                    usedTargetChars[j] = true;
                    break; // Используем букву только один раз
                }
            }
        }

        // Добавляем попытку в игру
        gameState.addGuess(wordGuess);

        // Проверяем результат
        if (upperGuess.equals(targetWord)) {
            gameState.setStatus(GameStatus.WON);
        } else if (gameState.getGuesses().size() >= 6) {
            gameState.setStatus(GameStatus.LOST);
        }

        return wordGuess;
    }

    /**
     * Получает случайное слово для тестирования
     */
    public String getRandomWord() {
        Random random = new Random();
        return wordList.get(random.nextInt(wordList.size()));
    }
}
