package org.example.wordle.controller;

import org.example.wordle.model.GameState;
import org.example.wordle.model.GameMode;
import org.example.wordle.model.WordGuess;
import org.example.wordle.model.DailyStats;
import org.example.wordle.service.WordleService;
import org.example.wordle.service.DailyWordService;
import org.example.wordle.service.FriendGameService;
import org.example.wordle.service.PersistentPlayerIdService;
import org.example.wordle.service.PlayerStatsService;
import org.example.wordle.service.PlayerStatsMigrationService;
import org.example.wordle.model.PlayerStatsEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @Autowired
    private PersistentPlayerIdService persistentPlayerIdService;
    
    @Autowired
    private PlayerStatsService playerStatsService;
    
    @Autowired
    private PlayerStatsMigrationService playerStatsMigrationService;
    
    @Value("${app.domain}")
    private String appDomain;
    
    /**
     * Получает playerId для текущей сессии, создавая его при необходимости.
     * Оптимизирован для избежания избыточных проверок в рамках одной сессии.
     * 
     * Логика:
     * 1. Сначала проверяем сессию (быстро)
     * 2. Если нет в сессии, используем PersistentPlayerIdService (куки + создание)
     * 3. Сохраняем результат в сессию для последующих запросов
     */
    private String getPlayerIdForSession(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        // Сначала проверяем, есть ли уже playerId в сессии
        String playerId = (String) session.getAttribute("wordle_player_id");
        
        if (playerId != null && !playerId.isEmpty()) {
            // PlayerId уже есть в сессии, используем его (оптимизация)
            return playerId;
        }
        
        // PlayerId нет в сессии, получаем или создаем через PersistentPlayerIdService
        playerId = persistentPlayerIdService.getOrCreatePlayerId(request, response);
        
        // Сохраняем в сессию для быстрого доступа в рамках этой сессии
        session.setAttribute("wordle_player_id", playerId);
        
        return playerId;
    }
    
    /**
     * Главная страница игры
     */
    @GetMapping("/")
    public String index(@RequestParam(required = false) String word_id, Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        GameState gameState = (GameState) session.getAttribute("gameState");
        
        // Если передан word_id, создаем игру с загаданным словом
        if (word_id != null && !word_id.isEmpty()) {
            String friendWord = friendGameService.getFriendWord(word_id);
            if (friendWord != null) {
                // Создаем игру в режиме GUESS с загаданным словом
                gameState = new GameState(friendWord, GameMode.GUESS);
                gameState.setPlayerId(getPlayerIdForSession(request, response, session));
                gameState.setFriendGame(true); // Устанавливаем флаг игры с другом
                session.setAttribute("gameState", gameState);
                
                System.out.println("🎯 Создана игра с другом по слову: " + friendWord + " (ID: " + word_id + ")");
                
                model.addAttribute("gameState", gameState);
                model.addAttribute("todayDate", dailyWordService.getTodayDateString());
                model.addAttribute("gameNumber", dailyWordService.getTodayGameNumber());
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
            // Создаем игру с постоянным ID игрока
            String playerId = getPlayerIdForSession(request, response, session);
            gameState = wordleService.createGame(GameMode.DAILY, session);
            gameState.setPlayerId(playerId);
            session.setAttribute("gameState", gameState);
            
            System.out.println("🎮 Создана новая игра для игрока: " + playerId);
        }
        
        model.addAttribute("gameState", gameState);
        model.addAttribute("todayDate", dailyWordService.getTodayDateString());
        model.addAttribute("gameNumber", dailyWordService.getTodayGameNumber());
        model.addAttribute("playerId", gameState.getPlayerId());
        model.addAttribute("appDomain", appDomain);
        return "index";
    }
    
    /**
     * Обработка попытки угадать слово (REST API)
     */
    @PostMapping("/api/guess")
    @ResponseBody
    public Map<String, Object> makeGuessApi(@RequestParam String word, @RequestParam(required = false) Integer gameTimeSeconds, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> responseMap = new HashMap<>();
        GameState gameState = (GameState) session.getAttribute("gameState");
        
        if (gameState == null) {
            String playerId = getPlayerIdForSession(request, response, session);
            gameState = wordleService.createNewGame(session);
            gameState.setPlayerId(playerId);
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
                responseMap.put("success", false);
                responseMap.put("error", "Слово должно содержать ровно 5 букв");
            } else if (!wordleService.isValidWord(word, gameState)) {
                // Логируем попытку пользователя ввести невалидное слово
                responseMap.put("success", false);
                responseMap.put("error", "Введено неизвестное слово");
            } else {
                // Логируем успешную попытку пользователя
                WordGuess guess = wordleService.processGuess(word, gameState);
                responseMap.put("success", true);
                responseMap.put("gameState", gameState);
                responseMap.put("lastGuess", guess);
                System.out.println("Guess processed successfully");
            }
        } catch (IllegalStateException e) {
            System.err.println("IllegalStateException: " + e.getMessage());
            e.printStackTrace();
            responseMap.put("success", false);
            responseMap.put("error", e.getMessage());
        } catch (Exception e) {
            System.err.println("Exception in makeGuessApi: " + e.getMessage());
            e.printStackTrace();
            responseMap.put("success", false);
            responseMap.put("error", "Произошла ошибка: " + e.getMessage());
        }
        
        return responseMap;
    }

    /**
     * Обработка попытки угадать слово (для формы)
     */
    @PostMapping("/guess")
    public String makeGuess(@RequestParam String word, HttpSession session, Model model, HttpServletRequest request, HttpServletResponse response) {
        GameState gameState = (GameState) session.getAttribute("gameState");
        
        if (gameState == null) {
            String playerId = getPlayerIdForSession(request, response, session);
            gameState = wordleService.createNewGame(session);
            gameState.setPlayerId(playerId);
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
    public String newGame(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        GameState currentGame = (GameState) session.getAttribute("gameState");
        GameMode currentMode = (currentGame != null) ? currentGame.getGameMode() : GameMode.DAILY;
        
        // Получаем playerId для сессии
        String playerId = getPlayerIdForSession(request, response, session);
        
        GameState newGame = wordleService.createGame(currentMode, session);
        newGame.setPlayerId(playerId); // Устанавливаем постоянный ID
        session.setAttribute("gameState", newGame);
        
        System.out.println("🔄 Создана новая игра для игрока: " + playerId);
        return "redirect:/";
    }
    
    /**
     * Переключить режим игры
     */
    @PostMapping("/switch-mode")
    public String switchMode(@RequestParam String mode, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        try {
            System.out.println("Switching mode to: " + mode);
            GameMode gameMode = GameMode.valueOf(mode.toUpperCase());
            
            // Получаем playerId для сессии
            String playerId = getPlayerIdForSession(request, response, session);
            
            GameState newGame = wordleService.createGame(gameMode, session);
            newGame.setPlayerId(playerId); // Устанавливаем постоянный ID
            session.setAttribute("gameState", newGame);
            
            System.out.println("Mode switched successfully to: " + gameMode + " for player: " + playerId);
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
     * Получает информацию о текущем игроке
     */
    @GetMapping("/api/player/info")
    @ResponseBody
    public Map<String, Object> getPlayerInfo(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        String playerId = persistentPlayerIdService.getExistingPlayerId(request);
        String playerName = persistentPlayerIdService.getPlayerName(request);
        
        response.put("playerId", playerId);
        response.put("playerName", playerName);
        response.put("hasPlayer", playerId != null);
        
        return response;
    }
    
    /**
     * Устанавливает имя игрока
     */
    @PostMapping("/api/player/set-name")
    @ResponseBody
    public Map<String, Object> setPlayerName(@RequestParam String name, HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> responseMap = new HashMap<>();
        
        if (name == null || name.trim().isEmpty()) {
            responseMap.put("success", false);
            responseMap.put("message", "Имя не может быть пустым");
            return responseMap;
        }
        
        persistentPlayerIdService.setPlayerName(request, response, name);
        responseMap.put("success", true);
        responseMap.put("message", "Имя установлено: " + name.trim());
        responseMap.put("playerName", name.trim());
        
        return responseMap;
    }
    
    /**
     * Сбрасывает ID игрока (для тестирования)
     */
    @PostMapping("/api/player/reset")
    @ResponseBody
    public Map<String, Object> resetPlayer(HttpServletRequest request, HttpServletResponse response) {
        persistentPlayerIdService.resetPlayerId(request, response);
        
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "ID игрока сброшен (сессия + cookie)");
        
        return responseMap;
    }
    
    /**
     * Получает информацию о cookies игрока (для отладки)
     */
    @GetMapping("/api/player/cookie-info")
    @ResponseBody
    public Map<String, Object> getCookieInfo(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        String cookieInfo = persistentPlayerIdService.getPlayerCookieInfo(request);
        response.put("cookieInfo", cookieInfo);
        return response;
    }
    
    /**
     * Проверяет, может ли игрок играть сегодня в режиме дня
     */
    @GetMapping("/api/daily-game/can-play")
    @ResponseBody
    public Map<String, Object> canPlayDailyGame(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        String playerId = persistentPlayerIdService.getExistingPlayerId(request);
        
        if (playerId == null) {
            response.put("canPlay", false);
            response.put("reason", "Игрок не найден");
            return response;
        }
        
        boolean canPlay = wordleService.canPlayerPlayToday(playerId);
        response.put("canPlay", canPlay);
        
        if (!canPlay) {
            response.put("reason", wordleService.getPlayRestrictionReason(playerId));
        }
        
        return response;
    }
    
    /**
     * Получает статистику игрока за сегодня
     */
    @GetMapping("/api/daily-game/today-stats")
    @ResponseBody
    public Map<String, Object> getTodayPlayerStats(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        String playerId = persistentPlayerIdService.getExistingPlayerId(request);
        
        if (playerId == null) {
            response.put("error", "Игрок не найден");
            return response;
        }
        
        String stats = wordleService.getTodayPlayerStats(playerId);
        response.put("stats", stats);
        
        return response;
    }
    
    /**
     * Получает информацию о времени для игры
     */
    @GetMapping("/api/daily-game/time-info")
    @ResponseBody
    public Map<String, Object> getTimeInfo() {
        Map<String, Object> response = new HashMap<>();
        String timeInfo = wordleService.getTimeInfo();
        response.put("timeInfo", timeInfo);
        return response;
    }
    
    /**
     * Получает отладочную информацию о времени (для проверки часового пояса)
     */
    @GetMapping("/api/debug/timezone")
    @ResponseBody
    public Map<String, Object> getTimezoneDebugInfo() {
        Map<String, Object> response = new HashMap<>();
        
        // Получаем московское время через сервис
        String moscowTimeInfo = wordleService.getTimeInfo();
        response.put("moscowTimeInfo", moscowTimeInfo);
        
        // Системная информация
        response.put("systemTimezone", java.time.ZoneId.systemDefault().toString());
        response.put("defaultTimeZone", java.util.TimeZone.getDefault().getID());
        
        // Текущее время в разных форматах
        response.put("utcTime", java.time.Instant.now().toString());
        response.put("localTime", java.time.LocalDateTime.now().toString());
        response.put("moscowTime", java.time.LocalDateTime.now(java.time.ZoneId.of("Europe/Moscow")).toString());
        
        return response;
    }
    
    /**
     * Получает отладочную информацию о статистике (для проверки target word)
     */
    @GetMapping("/api/debug/stats")
    @ResponseBody
    public Map<String, Object> getStatsDebugInfo() {
        Map<String, Object> response = new HashMap<>();
        
        // Получаем слово дня
        String todayWord = dailyWordService.getTodayWord();
        response.put("todayWord", todayWord);
        
        // Получаем статистику дня
        try {
            DailyStats dailyStats = wordleService.getDailyStats();
            response.put("dailyStats", dailyStats);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Получает отладочную информацию о игроке (для проверки постоянства)
     */
    @GetMapping("/api/debug/player")
    @ResponseBody
    public Map<String, Object> getPlayerDebugInfo(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> responseMap = new HashMap<>();
        
        // Получаем информацию о игроке
        String playerId = persistentPlayerIdService.getExistingPlayerId(request);
        String playerName = persistentPlayerIdService.getPlayerName(request);
        boolean hasPlayer = persistentPlayerIdService.hasPlayer(request);
        
        // Получаем информацию о куки
        String cookieInfo = persistentPlayerIdService.getPlayerCookieInfo(request);
        
        // Получаем информацию о сессии
        HttpSession session = request.getSession(false);
        String sessionPlayerId = null;
        if (session != null) {
            sessionPlayerId = (String) session.getAttribute("wordle_player_id");
        }
        
        responseMap.put("playerId", playerId);
        responseMap.put("playerName", playerName);
        responseMap.put("hasPlayer", hasPlayer);
        responseMap.put("sessionPlayerId", sessionPlayerId);
        responseMap.put("cookieInfo", cookieInfo);
        responseMap.put("sessionExists", session != null);
        responseMap.put("sessionId", session != null ? session.getId() : "No session");
        
        return responseMap;
    }
    
    /**
     * Принудительно создает нового игрока (для тестирования)
     */
    @PostMapping("/api/debug/create-player")
    @ResponseBody
    public Map<String, Object> createNewPlayer(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> responseMap = new HashMap<>();
        
        // Сбрасываем текущего игрока
        persistentPlayerIdService.resetPlayerId(request, response);
        
        // Создаем нового игрока
        String newPlayerId = persistentPlayerIdService.getOrCreatePlayerId(request, response);
        
        responseMap.put("success", true);
        responseMap.put("message", "Создан новый игрок");
        responseMap.put("playerId", newPlayerId);
        
        return responseMap;
    }
    
    /**
     * Получает персональную статистику игрока
     */
    @GetMapping("/api/stats/player")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPlayerStats(HttpServletRequest request) {
        try {
            String playerId = persistentPlayerIdService.getExistingPlayerId(request);
            
            if (playerId == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Игрок не найден");
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            PlayerStatsEntity playerStats = playerStatsService.getPlayerStats(playerId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("playerStats", playerStats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error getting player stats: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Ошибка получения статистики игрока");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Пересчитывает персональную статистику игрока
     */
    @PostMapping("/api/stats/player/recalculate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recalculatePlayerStats(HttpServletRequest request) {
        try {
            String playerId = persistentPlayerIdService.getExistingPlayerId(request);
            
            if (playerId == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Игрок не найден");
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            playerStatsService.recalculatePlayerStats(playerId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Статистика пересчитана");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error recalculating player stats: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Ошибка пересчета статистики");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Запускает миграцию персональной статистики
     */
    @PostMapping("/api/stats/migrate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> migratePlayerStats() {
        try {
            if (!playerStatsMigrationService.needsMigration()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Миграция не нужна - данные уже существуют");
                return ResponseEntity.ok(response);
            }
            
            playerStatsMigrationService.migratePlayerStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Миграция персональной статистики завершена");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error migrating player stats: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Ошибка миграции: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

}
