package org.example.wordle.service;

import org.example.wordle.model.*;
import org.example.wordle.repository.WordsRepository;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;


/**
 * Сервис для логики игры Wordle
 */
@Service
public class WordleService {

    private final WordsRepository wordsRepository;
    private final DailyWordService dailyWordService;
    private final DictionaryApiService dictionaryApiService;
    private final StatsService statsService;
    private final PlayerIdService playerIdService;
    private final LocalTimeService localTimeService;

    public WordleService(WordsRepository wordsRepository, 
                        DailyWordService dailyWordService,
                        DictionaryApiService dictionaryApiService,
                        StatsService statsService,
                        PlayerIdService playerIdService,
                        LocalTimeService localTimeService) {
        this.wordsRepository = wordsRepository;
        this.dailyWordService = dailyWordService;
        this.dictionaryApiService = dictionaryApiService;
        this.statsService = statsService;
        this.playerIdService = playerIdService;
        this.localTimeService = localTimeService;
    }

    /**
     * Создает новую игру со случайным словом в режиме угадывания
     * Слово проверяется через Яндекс API для валидности
     */
    public GameState createNewGame(HttpSession session) {
        String targetWord = getValidRandomWord();
        GameState gameState = new GameState(targetWord, GameMode.GUESS);
        gameState.setPlayerId(playerIdService.getOrCreatePlayerId(session));
        return gameState;
    }

    /**
     * Создает новую игру в режиме слова дня
     */
    public GameState createDailyGame(HttpSession session) {
        String targetWord = dailyWordService.getTodayWord();
        GameState gameState = new GameState(targetWord, GameMode.DAILY);
        gameState.setPlayerId(playerIdService.getOrCreatePlayerId(session));
        return gameState;
    }

    /**
     * Создает игру в указанном режиме
     */
    public GameState createGame(GameMode mode, HttpSession session) {
        if (mode == GameMode.DAILY) {
            return createDailyGame(session);
        } else {
            return createNewGame(session);
        }
    }

    /**
     * Проверяет, является ли слово валидным (через API словари с fallback)
     */
    public boolean isValidWord(String word) {
        if (word == null || word.length() != 5) {
            return false;
        }
        
        // Проверяем формат слова
        if (!word.matches("[а-яА-ЯёЁ]{5}")) {
            return false;
        }
        
        // Используем API сервис с fallback на локальный словарь
        return dictionaryApiService.isWordValid(word);
    }

    /**
     * Проверяет, является ли слово валидным для конкретной игры
     * В режиме "Игра с другом" валидация через Яндекс API отключена
     */
    public boolean isValidWord(String word, GameState gameState) {
        if (word == null || word.length() != 5) {
            return false;
        }
        
        // Проверяем формат слова
        if (!word.matches("[а-яА-ЯёЁ]{5}")) {
            return false;
        }
        
        // В режиме "Игра с другом" не проверяем через Яндекс API
        if (gameState.getGameMode() == GameMode.GUESS && gameState.isFriendGame()) {
            System.out.println("🎯 Режим 'Игра с другом': валидация через Яндекс API отключена для слова: " + word);
            return true; // Принимаем любое 5-буквенное русское слово
        }
        
        // Для остальных режимов используем полную валидацию
        return dictionaryApiService.isWordValid(word);
    }
    
    /**
     * Проверяет, является ли слово загадываемым словом (есть в списке)
     */
    public boolean isTargetWord(String word) {
        if (word == null || word.length() != 5) {
            return false;
        }
        
        String normalizedWord = normalizeWord(word);
        return wordsRepository.isTargetWord(normalizedWord);
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
     * Проверяет, являются ли две буквы эквивалентными (Е и Ё считаются одинаковыми)
     */
    private boolean areEquivalentLetters(char letter1, char letter2) {
        // Нормализуем обе буквы и сравниваем
        char norm1 = normalizeWord(String.valueOf(letter1)).charAt(0);
        char norm2 = normalizeWord(String.valueOf(letter2)).charAt(0);
        return norm1 == norm2;
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
            if (guessChar == targetChars[i] || areEquivalentLetters(guessChar, targetChars[i])) {
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
                if (!usedTargetChars[j] && (targetChars[j] == guessChar || areEquivalentLetters(guessChar, targetChars[j]))) {
                    wordGuess.getLetters().get(i).setState(LetterState.PRESENT);
                    usedTargetChars[j] = true;
                    break; // Используем букву только один раз
                }
            }
        }

        // Добавляем попытку в игру
        gameState.addGuess(wordGuess);

        // Проверяем результат - используем нормализованное сравнение для Е/Ё
        if (upperGuess.equals(targetWord) || normalizeWord(upperGuess).equals(normalizeWord(targetWord))) {
            gameState.setStatus(GameStatus.WON);
            // Записываем статистику для режима дня
            if (gameState.getGameMode() == GameMode.DAILY) {
                recordGameStats(gameState, true);
            }
        } else if (gameState.getGuesses().size() >= 6) {
            gameState.setStatus(GameStatus.LOST);
            // Записываем статистику для режима дня (неудачная игра)
            if (gameState.getGameMode() == GameMode.DAILY) {
                recordGameStats(gameState, false);
            }
        }

        return wordGuess;
    }

    /**
     * Получает случайное слово для тестирования
     */
    public String getRandomWord() {
        return wordsRepository.getRandomFiveLetterWord();
    }
    
    /**
     * Получает статистику словаря
     */
    public String getDictionaryStats() {
        return String.format("API словари: %s", dictionaryApiService.getApiStatus());
    }
    
    /**
     * Записывает статистику игры
     */
    private boolean recordGameStats(GameState gameState, boolean success) {
        if (gameState.getGameMode() != GameMode.DAILY) {
            return false; // Статистика только для режима дня
        }
        
        int attempts = success ? gameState.getGuesses().size() : 0;
        String playerId = gameState.getPlayerId();
        int gameTimeSeconds = gameState.getGameTimeSeconds();
        
        // Используем московское время для даты
        LocalDate gameDate = localTimeService.getCurrentMoscowDate();
        
        // ВАЖНО: Для режима дня всегда используем слово дня, а не targetWord из gameState
        // Это исправляет проблему, когда игрок играл с другом, но статистика записывается
        // с неправильным словом
        String targetWord = dailyWordService.getTodayWord();

        return statsService.recordGameStats(
            gameDate,
            attempts,
            playerId,
            targetWord, // Используем слово дня, а не из gameState
            gameTimeSeconds
        );
    }
    
    
    /**
     * Получает статистику дня
     */
    public DailyStats getDailyStats() {
        return statsService.getDailyStats(localTimeService.getCurrentMoscowDate());
    }
    
    /**
     * Получает статистику дня с результатом конкретного игрока
     */
    public DailyStats getDailyStatsWithPlayerResult(String playerId) {
        return statsService.getDailyStatsWithPlayerResult(localTimeService.getCurrentMoscowDate(), playerId);
    }
    
    /**
     * Получает статистику за последние дни
     */
    public List<DailyStats> getRecentStats(int days) {
        return statsService.getRecentStats(days);
    }
    
    /**
     * Получает информацию о состоянии статистики
     */
    public String getStatsInfo() {
        return statsService.getStatsInfo();
    }
    
    /**
     * Проверяет, может ли игрок играть сегодня в режиме дня
     */
    public boolean canPlayerPlayToday(String playerId) {
        return statsService.canPlayerPlayToday(playerId);
    }
    
    /**
     * Получает информацию о том, почему игрок не может играть
     */
    public String getPlayRestrictionReason(String playerId) {
        return statsService.getPlayRestrictionReason(playerId);
    }
    
    /**
     * Получает статистику игрока за сегодня
     */
    public String getTodayPlayerStats(String playerId) {
        return statsService.getTodayPlayerStats(playerId);
    }
    
    /**
     * Получает информацию о времени для игры
     */
    public String getTimeInfo() {
        return statsService.getTimeInfo();
    }
    
    /**
     * Получает валидное случайное слово с проверкой через Яндекс API
     * Если слово не проходит валидацию, выбирает следующее
     */
    private String getValidRandomWord() {
        List<String> fiveLetterWords = wordsRepository.getFiveLetterWords();
        int maxAttempts = Math.min(50, fiveLetterWords.size()); // Ограничиваем количество попыток
        
        System.out.println("Поиск валидного слова для режима УГАДЫВАТЬ...");
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String candidateWord = wordsRepository.getRandomFiveLetterWord();
            
            System.out.println("Проверяем слово для угадывания: " + candidateWord + " (попытка " + (attempt + 1) + ")");
            
            // Проверяем слово через Яндекс API
            if (dictionaryApiService.isWordValid(candidateWord)) {
                System.out.println("Слово для угадывания выбрано: " + candidateWord + " (валидация через Яндекс API пройдена)");
                return candidateWord;
            } else {
                System.out.println("Слово " + candidateWord + " не прошло валидацию через Яндекс API, пробуем следующее");
            }
        }
        
        // Если не удалось найти валидное слово через API, возвращаем случайное слово
        String fallbackWord = wordsRepository.getRandomFiveLetterWord();
        System.out.println("Не удалось найти валидное слово через API, используем fallback: " + fallbackWord);
        return fallbackWord;
    }
}
