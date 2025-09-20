package org.example.wordle.service;

import org.example.wordle.model.*;
import org.example.wordle.repository.WordsRepository;
import org.springframework.stereotype.Service;


/**
 * Сервис для логики игры Wordle
 */
@Service
public class WordleService {

    private final WordsRepository wordsRepository;
    private final DailyWordService dailyWordService;

    public WordleService(WordsRepository wordsRepository, DailyWordService dailyWordService) {
        this.wordsRepository = wordsRepository;
        this.dailyWordService = dailyWordService;
    }

    /**
     * Создает новую игру со случайным словом в режиме угадывания
     */
    public GameState createNewGame() {
        String targetWord = wordsRepository.getRandomFiveLetterWord();
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
        if (word == null || word.length() != 5) {
            return false;
        }
        
        // Проверяем только, что слово содержит только русские буквы
        return word.matches("[а-яА-ЯёЁ]{5}");
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
        return wordsRepository.getRandomFiveLetterWord();
    }
}
