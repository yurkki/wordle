package org.example.wordle.service;

import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;

/**
 * Сервис для постоянной идентификации игроков с использованием cookies
 */
@Service
public class PersistentPlayerIdService {
    
    private static final String PLAYER_ID_COOKIE_NAME = "wordle_player_id";
    private static final String PLAYER_NAME_COOKIE_NAME = "wordle_player_name";
    private static final String PLAYER_ID_SESSION_KEY = "wordle_player_id";
    private static final String PLAYER_NAME_SESSION_KEY = "wordle_player_name";
    
    // Время жизни cookie в днях (30 дней)
    private static final int COOKIE_MAX_AGE = 30 * 24 * 60 * 60;
    
    /**
     * Получает или создает постоянный ID игрока
     * Сначала проверяет cookie, затем сессию, затем создает новый
     */
    public String getOrCreatePlayerId(HttpServletRequest request, HttpServletResponse response) {
        // 1. Проверяем cookie
        String playerId = getPlayerIdFromCookie(request);
        
        if (playerId != null && !playerId.isEmpty()) {
            // Сохраняем в сессию для быстрого доступа
            HttpSession session = request.getSession();
            session.setAttribute(PLAYER_ID_SESSION_KEY, playerId);
            System.out.println("🔄 Восстановлен игрок из cookie: " + playerId);
            return playerId;
        }
        
        // 2. Проверяем сессию
        HttpSession session = request.getSession();
        playerId = (String) session.getAttribute(PLAYER_ID_SESSION_KEY);
        
        if (playerId != null && !playerId.isEmpty()) {
            // Сохраняем в cookie для постоянного хранения
            setPlayerIdCookie(response, playerId);
            return playerId;
        }
        
        // 3. Создаем новый ID
        playerId = generateUniquePlayerId();
        
        // Сохраняем в сессию и cookie
        session.setAttribute(PLAYER_ID_SESSION_KEY, playerId);
        setPlayerIdCookie(response, playerId);
        
        System.out.println("🆕 Создан новый постоянный игрок: " + playerId);
        return playerId;
    }
    
    /**
     * Получает существующий ID игрока
     */
    public String getExistingPlayerId(HttpServletRequest request) {
        // Сначала проверяем сессию (быстрее)
        HttpSession session = request.getSession(false);
        if (session != null) {
            String playerId = (String) session.getAttribute(PLAYER_ID_SESSION_KEY);
            if (playerId != null) {
                return playerId;
            }
        }
        
        // Затем проверяем cookie
        return getPlayerIdFromCookie(request);
    }
    
    /**
     * Устанавливает имя игрока
     */
    public void setPlayerName(HttpServletRequest request, HttpServletResponse response, String playerName) {
        if (playerName != null && !playerName.trim().isEmpty()) {
            String trimmedName = playerName.trim();
            
            // Сохраняем в сессию
            HttpSession session = request.getSession();
            session.setAttribute(PLAYER_NAME_SESSION_KEY, trimmedName);
            
            // Сохраняем в cookie
            setPlayerNameCookie(response, trimmedName);
            
            System.out.println("👤 Игрок " + getExistingPlayerId(request) + " установил имя: " + trimmedName);
        }
    }
    
    /**
     * Получает имя игрока
     */
    public String getPlayerName(HttpServletRequest request) {
        // Сначала проверяем сессию
        HttpSession session = request.getSession(false);
        if (session != null) {
            String playerName = (String) session.getAttribute(PLAYER_NAME_SESSION_KEY);
            if (playerName != null) {
                return playerName;
            }
        }
        
        // Затем проверяем cookie
        return getPlayerNameFromCookie(request);
    }
    
    /**
     * Сбрасывает ID игрока (удаляет из сессии и cookie)
     */
    public void resetPlayerId(HttpServletRequest request, HttpServletResponse response) {
        // Очищаем сессию
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(PLAYER_ID_SESSION_KEY);
            session.removeAttribute(PLAYER_NAME_SESSION_KEY);
        }
        
        // Очищаем cookies
        clearPlayerIdCookie(response);
        clearPlayerNameCookie(response);
        
        System.out.println("🔄 ID игрока сброшен (сессия + cookie)");
    }
    
    /**
     * Проверяет, есть ли игрок
     */
    public boolean hasPlayer(HttpServletRequest request) {
        return getExistingPlayerId(request) != null;
    }
    
    /**
     * Получает ID игрока из cookie
     */
    private String getPlayerIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (PLAYER_ID_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    /**
     * Получает имя игрока из cookie
     */
    private String getPlayerNameFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (PLAYER_NAME_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    /**
     * Устанавливает cookie с ID игрока
     */
    private void setPlayerIdCookie(HttpServletResponse response, String playerId) {
        Cookie cookie = new Cookie(PLAYER_ID_COOKIE_NAME, playerId);
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setPath("/");
        cookie.setHttpOnly(true); // Защита от XSS
        cookie.setSecure(false); // Для HTTP (в продакшене должно быть true)
        response.addCookie(cookie);
    }
    
    /**
     * Устанавливает cookie с именем игрока
     */
    private void setPlayerNameCookie(HttpServletResponse response, String playerName) {
        Cookie cookie = new Cookie(PLAYER_NAME_COOKIE_NAME, playerName);
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setPath("/");
        cookie.setHttpOnly(false); // Доступно для JavaScript
        cookie.setSecure(false); // Для HTTP (в продакшене должно быть true)
        response.addCookie(cookie);
    }
    
    /**
     * Очищает cookie с ID игрока
     */
    private void clearPlayerIdCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(PLAYER_ID_COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
    
    /**
     * Очищает cookie с именем игрока
     */
    private void clearPlayerNameCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(PLAYER_NAME_COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
    
    /**
     * Генерирует уникальный ID игрока
     */
    private String generateUniquePlayerId() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "player_" + uuid.substring(0, 12);
    }
    
    /**
     * Получает информацию о cookies игрока
     */
    public String getPlayerCookieInfo(HttpServletRequest request) {
        StringBuilder info = new StringBuilder();
        info.append("🍪 Информация о cookies игрока:\n");
        
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            info.append("   Cookies не найдены\n");
            return info.toString();
        }
        
        boolean hasPlayerId = false;
        boolean hasPlayerName = false;
        
        for (Cookie cookie : cookies) {
            if (PLAYER_ID_COOKIE_NAME.equals(cookie.getName())) {
                info.append("   Player ID: ").append(cookie.getValue()).append("\n");
                hasPlayerId = true;
            } else if (PLAYER_NAME_COOKIE_NAME.equals(cookie.getName())) {
                info.append("   Player Name: ").append(cookie.getValue()).append("\n");
                hasPlayerName = true;
            }
        }
        
        if (!hasPlayerId) {
            info.append("   Player ID cookie не найден\n");
        }
        if (!hasPlayerName) {
            info.append("   Player Name cookie не найден\n");
        }
        
        return info.toString();
    }
}
