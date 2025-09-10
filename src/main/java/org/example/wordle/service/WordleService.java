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
            "БЕРЕГ", "БЕСЕДА", "БИЛЕТ", "БИРЖА", "БЛОКА", "БОГАТ", "БОЖИЙ", "БОКАЛ", "БОМБА", "БОРОН",
            "ВАГОН", "ВАЗОН", "ВАЛЕТ", "ВАЛЬС", "ВАННА", "ВЕСНА", "ВЕТКА", "ВИДЕО", "ВИЛКА", "ВИНТА",
            "ВОЗДУХ", "ВОЛНА", "ВОРОТ", "ВОСТОК", "ВЫБОР", "ВЫСОТ", "ГАЗОН", "ГАММА", "ГАРАЖ", "ГВОЗД",
            "ГЕРОЙ", "ГИБКА", "ГЛАЗА", "ГНЕЗДО", "ГОЛОС", "ГОРКА", "ГРАДУ", "ГРУША", "ГУСАК", "ДАВКА",
            "ДАЧА", "ДВОРЕ", "ДЕБЕТ", "ДЕВКА", "ДЕЛО", "ДЕНЬГИ", "ДЕРЕВО", "ДЕТКА", "ДИВАН", "ДОБРО",
            "ДОМА", "ДОРОГА", "ДОСКА", "ДОЧКА", "ДРУГ", "ДУБОК", "ДУМА", "ДУХ", "ДЫМ", "ДЯДЯ",
            "ЕВРО", "ЕДА", "ЕЗДА", "ЕЛКА", "ЕМА", "ЕНОТ", "ЕПАРХ", "ЕРЕСЬ", "ЕСЛИ", "ЕСТЬ",
            "ЖАБА", "ЖАЛО", "ЖАР", "ЖАТВА", "ЖЕЛЕ", "ЖЕЛТЫЙ", "ЖЕНА", "ЖИВОТ", "ЖИР", "ЖУК",
            "ЗАБОР", "ЗАВОД", "ЗАГАР", "ЗАДОК", "ЗАЕМ", "ЗАКАЗ", "ЗАЛ", "ЗАМОК", "ЗАПАД", "ЗАРЯ",
            "ЗВЕЗДА", "ЗВОНОК", "ЗЕБРА", "ЗЕМЛЯ", "ЗИМА", "ЗЛО", "ЗМЕЯ", "ЗОЛОТО", "ЗУБ", "ЗЯБЬ",
            "ИГЛА", "ИДЕАЛ", "ИЗБА", "ИЗЮМ", "ИКОНА", "ИМЯ", "ИНЕЙ", "ИОНИЙ", "ИРИС", "ИСТОК",
            "КАБАН", "КАДР", "КАЗАК", "КАЙФ", "КАМЕНЬ", "КАНАЛ", "КАПЛЯ", "КАРТА", "КАТОК", "КВАС",
            "КЕДР", "КИНО", "КЛАСС", "КЛЮЧ", "КНИГА", "КОЖА", "КОЛОС", "КОНЕЦ", "КОПИЯ", "КОРОВА",
            "КРАСНЫЙ", "КРЕСТ", "КРОВЬ", "КРУГ", "КУКЛА", "КУРС", "ЛАВКА", "ЛАДОНЬ", "ЛАМПА", "ЛЕВ",
            "ЛЕД", "ЛЕС", "ЛЕТО", "ЛИМОН", "ЛИСТ", "ЛОЖКА", "ЛУК", "ЛУНА", "ЛЮБОВЬ", "МАГИЯ",
            "МАЙ", "МАК", "МАМА", "МАРТ", "МАСЛО", "МАТЬ", "МЕД", "МЕЛЬ", "МЕСТО", "МЕТРО",
            "МИР", "МОРЕ", "МОСТ", "МУЗЫКА", "МЫЛО", "МЯЧ", "НАРОД", "НЕБО", "НЕДРА", "НЕФТЬ",
            "НОГА", "НОС", "НОЧЬ", "НУЖДА", "ОБЕД", "ОБЛАКО", "ОГОНЬ", "ОДЕЖДА", "ОЗЕРО", "ОКНО",
            "ОЛЕНЬ", "ОМАР", "ОПЕРА", "ОРЕХ", "ОСЕНЬ", "ОТЕЦ", "ОЧКИ", "ПАПА", "ПАРК", "ПАСХА",
            "ПЕСНЯ", "ПИВО", "ПИЛА", "ПИСЬМО", "ПЛАН", "ПЛОД", "ПОЛ", "ПОЛЕ", "ПОМОЩЬ", "ПОРОГ",
            "ПОТ", "ПОЧТА", "ПРАВДА", "ПРИЗ", "ПРОБА", "ПРУД", "ПТИЦА", "ПУТЬ", "ПЫЛЬ", "ПЯТЬ",
            "РАБОТА", "РАДИО", "РАЙ", "РАК", "РАМА", "РЕКА", "РЕСУРС", "РИС", "РОЗА", "РУКА",
            "РЫБА", "РЫНОК", "САД", "САЛАТ", "САМОЛЕТ", "САНКИ", "СВЕТ", "СЕЛО", "СЕМЬЯ", "СИЛА",
            "СКАЗКА", "СЛОВО", "СМЕХ", "СНЕГ", "СОБАКА", "СОЛНЦЕ", "СОН", "СОСНА", "СОТНЯ", "СПОРТ",
            "СТОЛ", "СТУЛ", "СУД", "СУМКА", "СУП", "СУТКИ", "СЧАСТЬЕ", "ТАБЛО", "ТАКСИ", "ТАЛОН",
            "ТАНЕЦ", "ТАРЕЛКА", "ТЕАТР", "ТЕЛО", "ТЕМА", "ТЕНЬ", "ТЕПЛО", "ТЕСТ", "ТИГР", "ТОЛПА",
            "ТОМ", "ТОН", "ТОПОР", "ТОРТ", "ТОЧКА", "ТРАВА", "ТРУБА", "ТУЧА", "ТЫКВА", "УГОЛ",
            "УДАР", "УЖАС", "УЗЕЛ", "УКРАС", "УЛИЦА", "УМ", "УПОР", "УРОК", "УСПЕХ", "УТКА",
            "ФАКТ", "ФАРС", "ФАСОЛЬ", "ФИЛЬМ", "ФИНИШ", "ФЛАГ", "ФОТО", "ФРАЗА", "ФРУКТ", "ФУТБОЛ",
            "ХАЛАТ", "ХАРЧИ", "ХВОСТ", "ХИМИЯ", "ХЛЕБ", "ХОБОТ", "ХОЛОД", "ХОРОШО", "ХРУСТ", "ЦВЕТ",
            "ЦЕЛЬ", "ЦЕНА", "ЦЕПЬ", "ЦИРК", "ЦУНАМИ", "ЧАЙ", "ЧАС", "ЧЕЛОВЕК", "ЧЕРТ", "ЧИСТЫЙ",
            "ЧУДО", "ШАГ", "ШАПКА", "ШЕЯ", "ШИНА", "ШКОЛА", "ШЛЯПА", "ШОК", "ШУМ", "ЩЕКА",
            "ЩИТ", "ЭКРАН", "ЭКСПО", "ЭЛЕКТРО", "ЭМАЛЬ", "ЭПОХА", "ЭТАЖ", "ЭТО", "ЮБКА", "ЮГ",
            "ЮМОР", "ЯБЛОКО", "ЯВКА", "ЯД", "ЯЗЫК", "ЯМА", "ЯР", "ЯСЛИ", "ЯЩИК", "ЯЩУР"
        );
        
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
        
        // Проверяем каждую букву
        for (int i = 0; i < 5; i++) {
            char guessChar = upperGuess.charAt(i);
            char targetChar = targetWord.charAt(i);
            
            LetterGuess letterGuess = wordGuess.getLetters().get(i);
            
            if (guessChar == targetChar) {
                // Буква на правильном месте
                letterGuess.setState(LetterState.CORRECT);
            } else if (targetWord.indexOf(guessChar) != -1) {
                // Буква есть в слове, но на другом месте
                letterGuess.setState(LetterState.PRESENT);
            } else {
                // Буквы нет в слове
                letterGuess.setState(LetterState.ABSENT);
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
