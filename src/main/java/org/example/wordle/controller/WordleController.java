package org.example.wordle.controller;

import org.example.wordle.model.GameState;
import org.example.wordle.model.GameMode;
import org.example.wordle.model.WordGuess;
import org.example.wordle.model.DailyStats;
import org.example.wordle.service.WordleService;
import org.example.wordle.service.DailyWordService;
import org.example.wordle.service.FriendGameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Контроллер для игры Wordle
 */
@Controller
public class WordleController {
    
    @Autowired
    private WordleService wordleService;
    
    @Autowired
    private DailyWordService dailyWordService;
    
    @Autowired
    private FriendGameService friendGameService;
    
    @Value("${app.domain}")
    private String appDomain;
    
    /**
     * Главная страница игры
     */
    @GetMapping("/")
    public String index(@RequestParam(required = false) String word_id, Model model, HttpSession session) {
        GameState gameState = (GameState) session.getAttribute("gameState");
        
        // Если передан word_id, создаем игру с загаданным словом
        if (word_id != null && !word_id.isEmpty()) {
            String friendWord = friendGameService.getFriendWord(word_id);
            if (friendWord != null) {
                // Создаем игру в режиме GUESS с загаданным словом
                gameState = new GameState(friendWord, GameMode.GUESS);
                gameState.setPlayerId(generatePlayerId());
                session.setAttribute("gameState", gameState);
                
                System.out.println("🎯 Создана игра с другом по слову: " + friendWord + " (ID: " + word_id + ")");
                
                model.addAttribute("gameState", gameState);
                model.addAttribute("todayDate", dailyWordService.getTodayDateString());
                model.addAttribute("playerId", gameState.getPlayerId());
                model.addAttribute("friendGame", true);
                model.addAttribute("friendWordId", word_id);
                
                // Игра с другом создана
                
                return "index";
            } else {
                System.out.println("❌ Игра с ID " + word_id + " не найдена");
                model.addAttribute("error", "Игра не найдена или устарела");
            }
        }
        
        // Обычная логика для главной страницы
        if (gameState == null) {
            gameState = wordleService.createGame(GameMode.DAILY);
            session.setAttribute("gameState", gameState);
        }
        
        model.addAttribute("gameState", gameState);
        model.addAttribute("todayDate", dailyWordService.getTodayDateString());
        model.addAttribute("playerId", gameState.getPlayerId());
        return "index";
    }
    
    /**
     * Обработка попытки угадать слово (REST API)
     */
    @PostMapping("/api/guess")
    @ResponseBody
    public Map<String, Object> makeGuessApi(@RequestParam String word, @RequestParam(required = false) Integer gameTimeSeconds, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        GameState gameState = (GameState) session.getAttribute("gameState");
        
        if (gameState == null) {
            gameState = wordleService.createNewGame();
            session.setAttribute("gameState", gameState);
        }
        
        try {
            System.out.println("Received guess: " + word);
            System.out.println("Current game state: " + (gameState != null ? gameState.getGameMode() : "null"));
            
            // Обновляем время игры, если передано
            if (gameTimeSeconds != null) {
                gameState.setGameTimeSeconds(gameTimeSeconds);
            }
            
            if (word.length() != 5) {
                response.put("success", false);
                response.put("error", "Слово должно содержать ровно 5 букв");
            } else if (!wordleService.isValidWord(word)) {
                // Логируем попытку пользователя ввести невалидное слово
                response.put("success", false);
                response.put("error", "Введено неизвестное слово");
            } else {
                // Логируем успешную попытку пользователя
                WordGuess guess = wordleService.processGuess(word, gameState);
                response.put("success", true);
                response.put("gameState", gameState);
                response.put("lastGuess", guess);
                System.out.println("Guess processed successfully");
            }
        } catch (IllegalStateException e) {
            System.err.println("IllegalStateException: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("error", e.getMessage());
        } catch (Exception e) {
            System.err.println("Exception in makeGuessApi: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Произошла ошибка: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Обработка попытки угадать слово (для формы)
     */
    @PostMapping("/guess")
    public String makeGuess(@RequestParam String word, HttpSession session, Model model) {
        GameState gameState = (GameState) session.getAttribute("gameState");
        
        if (gameState == null) {
            gameState = wordleService.createNewGame();
            session.setAttribute("gameState", gameState);
        }
        
        try {
            if (word.length() != 5) {
                model.addAttribute("error", "Слово должно содержать ровно 5 букв");
            } else if (!wordleService.isValidWord(word)) {
                model.addAttribute("error", "Слово должно содержать только русские буквы");
            } else {
                WordGuess guess = wordleService.processGuess(word, gameState);
                model.addAttribute("lastGuess", guess);
            }
        } catch (IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
        }
        
        model.addAttribute("gameState", gameState);
        return "index";
    }
    
    /**
     * Начать новую игру
     */
    @PostMapping("/new-game")
    public String newGame(HttpSession session) {
        GameState currentGame = (GameState) session.getAttribute("gameState");
        GameMode currentMode = (currentGame != null) ? currentGame.getGameMode() : GameMode.DAILY;
        GameState newGame = wordleService.createGame(currentMode);
        session.setAttribute("gameState", newGame);
        return "redirect:/";
    }
    
    /**
     * Переключить режим игры
     */
    @PostMapping("/switch-mode")
    public String switchMode(@RequestParam String mode, HttpSession session) {
        try {
            System.out.println("Switching mode to: " + mode);
            GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
            GameState newGame = wordleService.createGame(gameMode);
            session.setAttribute("gameState", newGame);
            System.out.println("Mode switched successfully to: " + gameMode);
            return "redirect:/";
        } catch (Exception e) {
            System.err.println("Error switching mode: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/";
        }
    }
    
    /**
     * Получить подсказку (показать загаданное слово)
     */
    @GetMapping("/hint")
    @ResponseBody
    public String getHint(HttpSession session) {
        GameState gameState = (GameState) session.getAttribute("gameState");
        if (gameState != null) {
            return gameState.getTargetWord();
        }
        return "Игра не найдена";
    }
    
    
    /**
     * Простой endpoint для проверки запуска
     */
    @GetMapping("/status")
    @ResponseBody
    public ResponseEntity<Map<String, String>> status() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Wordle Game is running!");
        response.put("status", "UP");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        response.put("dictionary", wordleService.getDictionaryStats());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Получает статистику дня
     */
    @GetMapping("/api/stats/daily")
    @ResponseBody
    public ResponseEntity<DailyStats> getDailyStats() {
        try {
            DailyStats stats = wordleService.getDailyStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error getting daily stats: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Получает статистику дня с результатом конкретного игрока
     */
    @GetMapping("/api/stats/daily/player/{playerId}")
    @ResponseBody
    public ResponseEntity<DailyStats> getDailyStatsWithPlayer(@PathVariable String playerId) {
        try {
            DailyStats stats = wordleService.getDailyStatsWithPlayerResult(playerId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error getting daily stats for player: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Получает статистику за последние дни
     */
    @GetMapping("/api/stats/recent")
    @ResponseBody
    public ResponseEntity<List<DailyStats>> getRecentStats(@RequestParam(defaultValue = "7") int days) {
        try {
            List<DailyStats> stats = wordleService.getRecentStats(days);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error getting recent stats: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Получает позицию игрока в рейтинге дня
     */
    @GetMapping("/api/stats/player-rank")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPlayerRank(@RequestParam String playerId) {
        try {
            DailyStats stats = wordleService.getDailyStatsWithPlayerResult(playerId);
            Map<String, Object> response = new HashMap<>();
            
            if (stats.getPlayerResult() != null && stats.getPlayerResult().isSuccess()) {
                response.put("success", true);
                response.put("rank", stats.getPlayerResult().getRank());
                response.put("attempts", stats.getPlayerResult().getAttempts());
            } else {
                response.put("success", false);
                response.put("message", "Игрок не угадал слово или не играл сегодня");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error getting player rank: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Ошибка получения рейтинга");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Получает информацию о состоянии статистики
     */
    @GetMapping("/api/stats/info")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatsInfo() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("info", wordleService.getStatsInfo());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error getting stats info: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Ошибка получения информации о статистике");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Сохранить слово для игры с другом
     */
    @PostMapping("/api/friend/save-word")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveFriendWord(@RequestParam String word) {
        try {
            Map<String, Object> response = new HashMap<>();
            
            if (word == null || word.length() != 5) {
                response.put("success", false);
                response.put("error", "Слово должно содержать ровно 5 букв");
                return ResponseEntity.badRequest().body(response);
            }
            
            String wordId = friendGameService.saveFriendWord(word);
            response.put("success", true);
            response.put("word_id", wordId);
            
            System.out.println("✅ Сохранено слово для игры с другом: " + word + " (ID: " + wordId + ")");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error saving friend word: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Ошибка при сохранении слова");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Получить конфигурацию приложения
     */
    @GetMapping("/api/config")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAppConfig() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("domain", appDomain);
            
            System.out.println("📋 Конфигурация приложения: домен = " + appDomain);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error getting app config: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Ошибка получения конфигурации");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Генерирует уникальный ID игрока
     */
    private String generatePlayerId() {
        return "player_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

}
