package org.example.wordle.service;

import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;
import java.util.UUID;

/**
 * Сервис для управления идентификаторами игроков
 */
@Service
public class PlayerIdService {
    
    private static final String PLAYER_ID_SESSION_KEY = "wordle_player_id";
    private static final String PLAYER_NAME_SESSION_KEY = "wordle_player_name";
    
    /**
     * Получает или создает ID игрока для сессии
     * Если игрок уже есть в сессии - возвращает существующий ID
     * Если нет - создает новый уникальный ID
     */
    public String getOrCreatePlayerId(HttpSession session) {
        String playerId = (String) session.getAttribute(PLAYER_ID_SESSION_KEY);
        
        if (playerId == null || playerId.isEmpty()) {
            playerId = generateUniquePlayerId();
            session.setAttribute(PLAYER_ID_SESSION_KEY, playerId);
            System.out.println("🆕 Создан новый игрок: " + playerId);
        }
        
        return playerId;
    }
    
    /**
     * Получает существующий ID игрока из сессии
     * Возвращает null, если игрок не найден
     */
    public String getExistingPlayerId(HttpSession session) {
        return (String) session.getAttribute(PLAYER_ID_SESSION_KEY);
    }
    
    /**
     * Устанавливает имя игрока в сессии
     */
    public void setPlayerName(HttpSession session, String playerName) {
        if (playerName != null && !playerName.trim().isEmpty()) {
            session.setAttribute(PLAYER_NAME_SESSION_KEY, playerName.trim());
            System.out.println("👤 Игрок " + getExistingPlayerId(session) + " установил имя: " + playerName.trim());
        }
    }
    
    /**
     * Получает имя игрока из сессии
     */
    public String getPlayerName(HttpSession session) {
        return (String) session.getAttribute(PLAYER_NAME_SESSION_KEY);
    }
    
    /**
     * Сбрасывает ID игрока (для тестирования или смены игрока)
     */
    public void resetPlayerId(HttpSession session) {
        session.removeAttribute(PLAYER_ID_SESSION_KEY);
        session.removeAttribute(PLAYER_NAME_SESSION_KEY);
        System.out.println("🔄 ID игрока сброшен");
    }
    
    /**
     * Проверяет, есть ли игрок в сессии
     */
    public boolean hasPlayer(HttpSession session) {
        return getExistingPlayerId(session) != null;
    }
    
    /**
     * Генерирует уникальный ID игрока
     */
    private String generateUniquePlayerId() {
        // Используем UUID для гарантированной уникальности
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "player_" + uuid.substring(0, 12); // Берем первые 12 символов
    }
    
    /**
     * Генерирует читаемый ID игрока (для отображения)
     */
    public String generateReadablePlayerId() {
        return "Игрок_" + (int)(Math.random() * 10000);
    }
}
