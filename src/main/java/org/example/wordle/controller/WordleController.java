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
                gameState.setFriendGame(true); // Устанавливаем флаг игры с другом
                session.setAttribute("gameState", gameState);
                
                System.out.println("🎯 Создана игра с другом по слову: " + friendWord + " (ID: " + word_id + ")");
                
                model.addAttribute("gameState", gameState);
                model.addAttribute("todayDate", dailyWordService.getTodayDateString());
                model.addAttribute("playerId", gameState.getPlayerId());
                model.addAttribute("friendGame", true);
                model.addAttribute("friendWordId", word_id);
                model.addAttribute("appDomain", appDomain);
                
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
        model.addAttribute("appDomain", appDomain);
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
            } else if (!wordleService.isValidWord(word, gameState)) {
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
            } else if (!wordleService.isValidWord(word, gameState)) {
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
     * Генерировать превью изображение с динамическим доменом
     */
    @GetMapping("/images/wordle-preview.svg")
    @ResponseBody
    public ResponseEntity<String> getWordlePreview() {
        try {
            String domain = appDomain.replace("https://", "").replace("http://", "");
            String svg = generateWordlePreviewSvg(domain);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "image/svg+xml")
                    .body(svg);
        } catch (Exception e) {
            System.err.println("Error generating preview: " + e.getMessage());
            return ResponseEntity.status(500).body("Error generating preview");
        }
    }

    /**
     * Генерировать превью изображение для игры с друзьями
     */
    @GetMapping("/images/wordle-friend-game.svg")
    @ResponseBody
    public ResponseEntity<String> getFriendGamePreview() {
        try {
            String domain = appDomain.replace("https://", "").replace("http://", "");
            String svg = generateFriendGamePreviewSvg(domain);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "image/svg+xml")
                    .body(svg);
        } catch (Exception e) {
            System.err.println("Error generating friend game preview: " + e.getMessage());
            return ResponseEntity.status(500).body("Error generating preview");
        }
    }

    /**
     * Генерирует SVG для превью Wordle
     */
    private String generateWordlePreviewSvg(String domain) {
        return String.format("""
            <svg width="1200" height="630" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="bg" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
                  <stop offset="0%%" style="stop-color:#667eea;stop-opacity:1" />
                  <stop offset="100%%" style="stop-color:#764ba2;stop-opacity:1" />
                </linearGradient>
              </defs>
              
              <rect width="1200" height="630" fill="url(#bg)"/>
              
              <text x="600" y="200" font-family="Arial, sans-serif" font-size="80" font-weight="bold" text-anchor="middle" fill="white">WORDLE</text>
              <text x="600" y="280" font-family="Arial, sans-serif" font-size="32" text-anchor="middle" fill="white">Русская версия</text>
              <text x="600" y="350" font-family="Arial, sans-serif" font-size="24" text-anchor="middle" fill="white">Угадайте слово из 5 букв за 6 попыток</text>
              
              <g transform="translate(400, 400)">
                <rect x="0" y="0" width="60" height="60" fill="#6aaa64" stroke="#d3d6da" stroke-width="2"/>
                <text x="30" y="40" font-family="Arial, sans-serif" font-size="24" font-weight="bold" text-anchor="middle" fill="white">С</text>
                <rect x="70" y="0" width="60" height="60" fill="#c9b458" stroke="#d3d6da" stroke-width="2"/>
                <text x="100" y="40" font-family="Arial, sans-serif" font-size="24" font-weight="bold" text-anchor="middle" fill="white">Л</text>
                <rect x="140" y="0" width="60" height="60" fill="#6aaa64" stroke="#d3d6da" stroke-width="2"/>
                <text x="170" y="40" font-family="Arial, sans-serif" font-size="24" font-weight="bold" text-anchor="middle" fill="white">О</text>
                <rect x="210" y="0" width="60" height="60" fill="#6aaa64" stroke="#d3d6da" stroke-width="2"/>
                <text x="240" y="40" font-family="Arial, sans-serif" font-size="24" font-weight="bold" text-anchor="middle" fill="white">В</text>
                <rect x="280" y="0" width="60" height="60" fill="#6aaa64" stroke="#d3d6da" stroke-width="2"/>
                <text x="310" y="40" font-family="Arial, sans-serif" font-size="24" font-weight="bold" text-anchor="middle" fill="white">О</text>
              </g>
              
              <text x="600" y="550" font-family="Arial, sans-serif" font-size="20" text-anchor="middle" fill="white" opacity="0.8">%s</text>
            </svg>
            """, domain);
    }

    /**
     * Генерирует SVG для превью игры с друзьями
     */
    private String generateFriendGamePreviewSvg(String domain) {
        return String.format("""
            <svg width="1200" height="630" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="bg" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
                  <stop offset="0%%" style="stop-color:#ff6b6b;stop-opacity:1" />
                  <stop offset="100%%" style="stop-color:#ee5a24;stop-opacity:1" />
                </linearGradient>
              </defs>
              
              <rect width="1200" height="630" fill="url(#bg)"/>
              
              <text x="600" y="180" font-family="Arial, sans-serif" font-size="70" font-weight="bold" text-anchor="middle" fill="white">👥 ИГРА С ДРУГОМ</text>
              <text x="600" y="250" font-family="Arial, sans-serif" font-size="36" text-anchor="middle" fill="white">Твой друг загадал слово!</text>
              <text x="600" y="320" font-family="Arial, sans-serif" font-size="28" text-anchor="middle" fill="white">Попробуй отгадать его за 6 попыток</text>
              
              <g transform="translate(400, 380)">
                <rect x="0" y="0" width="50" height="50" fill="#6aaa64" stroke="#d3d6da" stroke-width="2"/>
                <text x="25" y="35" font-family="Arial, sans-serif" font-size="20" font-weight="bold" text-anchor="middle" fill="white">?</text>
                <rect x="60" y="0" width="50" height="50" fill="#c9b458" stroke="#d3d6da" stroke-width="2"/>
                <text x="85" y="35" font-family="Arial, sans-serif" font-size="20" font-weight="bold" text-anchor="middle" fill="white">?</text>
                <rect x="120" y="0" width="50" height="50" fill="#6aaa64" stroke="#d3d6da" stroke-width="2"/>
                <text x="145" y="35" font-family="Arial, sans-serif" font-size="20" font-weight="bold" text-anchor="middle" fill="white">?</text>
                <rect x="180" y="0" width="50" height="50" fill="#6aaa64" stroke="#d3d6da" stroke-width="2"/>
                <text x="205" y="35" font-family="Arial, sans-serif" font-size="20" font-weight="bold" text-anchor="middle" fill="white">?</text>
                <rect x="240" y="0" width="50" height="50" fill="#6aaa64" stroke="#d3d6da" stroke-width="2"/>
                <text x="265" y="35" font-family="Arial, sans-serif" font-size="20" font-weight="bold" text-anchor="middle" fill="white">?</text>
              </g>
              
              <text x="600" y="500" font-family="Arial, sans-serif" font-size="24" text-anchor="middle" fill="white" font-weight="bold">Нажми, чтобы начать игру!</text>
              <text x="600" y="550" font-family="Arial, sans-serif" font-size="18" text-anchor="middle" fill="white" opacity="0.8">%s</text>
            </svg>
            """, domain);
    }

    /**
     * Генерирует уникальный ID игрока
     */
    private String generatePlayerId() {
        return "player_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

}
