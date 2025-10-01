package org.example.wordle;

import org.example.wordle.service.PlayerIdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для сервиса идентификации игроков
 */
@SpringBootTest
public class PlayerIdServiceTest {

    private PlayerIdService playerIdService;
    private MockHttpSession session;

    @BeforeEach
    public void setUp() {
        playerIdService = new PlayerIdService();
        session = new MockHttpSession();
    }

    @Test
    public void testGetOrCreatePlayerId_FirstTime() {
        // Первый вызов должен создать новый ID
        String playerId1 = playerIdService.getOrCreatePlayerId(session);
        
        assertNotNull(playerId1);
        assertTrue(playerId1.startsWith("player_"));
        assertEquals(20, playerId1.length());
        
        // Второй вызов должен вернуть тот же ID
        String playerId2 = playerIdService.getOrCreatePlayerId(session);
        assertEquals(playerId1, playerId2);
    }

    @Test
    public void testGetExistingPlayerId_NoPlayer() {
        // Если игрока нет, должен вернуть null
        String playerId = playerIdService.getExistingPlayerId(session);
        assertNull(playerId);
    }

    @Test
    public void testGetExistingPlayerId_WithPlayer() {
        // Создаем игрока
        String playerId = playerIdService.getOrCreatePlayerId(session);
        
        // Получаем существующего игрока
        String existingPlayerId = playerIdService.getExistingPlayerId(session);
        assertEquals(playerId, existingPlayerId);
    }

    @Test
    public void testSetAndGetPlayerName() {
        // Создаем игрока
        playerIdService.getOrCreatePlayerId(session);
        
        // Устанавливаем имя
        String playerName = "Тестовый Игрок";
        playerIdService.setPlayerName(session, playerName);
        
        // Проверяем, что имя сохранилось
        String retrievedName = playerIdService.getPlayerName(session);
        assertEquals(playerName, retrievedName);
    }

    @Test
    public void testSetPlayerName_EmptyName() {
        // Создаем игрока
        playerIdService.getOrCreatePlayerId(session);
        
        // Пытаемся установить пустое имя
        playerIdService.setPlayerName(session, "");
        playerIdService.setPlayerName(session, "   ");
        playerIdService.setPlayerName(session, null);
        
        // Имя не должно быть установлено
        String retrievedName = playerIdService.getPlayerName(session);
        assertNull(retrievedName);
    }

    @Test
    public void testSetPlayerName_Trimmed() {
        // Создаем игрока
        playerIdService.getOrCreatePlayerId(session);
        
        // Устанавливаем имя с пробелами
        playerIdService.setPlayerName(session, "  Игрок  ");
        
        // Проверяем, что имя обрезано
        String retrievedName = playerIdService.getPlayerName(session);
        assertEquals("Игрок", retrievedName);
    }

    @Test
    public void testResetPlayerId() {
        // Создаем игрока и устанавливаем имя
        String playerId = playerIdService.getOrCreatePlayerId(session);
        playerIdService.setPlayerName(session, "Тест");
        
        // Проверяем, что данные есть
        assertTrue(playerIdService.hasPlayer(session));
        assertNotNull(playerIdService.getPlayerName(session));
        
        // Сбрасываем ID
        playerIdService.resetPlayerId(session);
        
        // Проверяем, что данные удалены
        assertFalse(playerIdService.hasPlayer(session));
        assertNull(playerIdService.getExistingPlayerId(session));
        assertNull(playerIdService.getPlayerName(session));
    }

    @Test
    public void testHasPlayer() {
        // Изначально игрока нет
        assertFalse(playerIdService.hasPlayer(session));
        
        // Создаем игрока
        playerIdService.getOrCreatePlayerId(session);
        
        // Теперь игрок есть
        assertTrue(playerIdService.hasPlayer(session));
    }

    @Test
    public void testPlayerIdUniqueness() {
        // Создаем несколько сессий
        MockHttpSession session1 = new MockHttpSession();
        MockHttpSession session2 = new MockHttpSession();
        
        // Получаем ID для каждой сессии
        String playerId1 = playerIdService.getOrCreatePlayerId(session1);
        String playerId2 = playerIdService.getOrCreatePlayerId(session2);
        
        // ID должны быть разными
        assertNotEquals(playerId1, playerId2);
        
        // ID должны быть уникальными при повторных вызовах
        String playerId1Again = playerIdService.getOrCreatePlayerId(session1);
        String playerId2Again = playerIdService.getOrCreatePlayerId(session2);
        
        assertEquals(playerId1, playerId1Again);
        assertEquals(playerId2, playerId2Again);
    }

    @Test
    public void testPlayerIdFormat() {
        String playerId = playerIdService.getOrCreatePlayerId(session);
        
        // Проверяем формат
        assertTrue(playerId.startsWith("player_"));
        assertEquals(20, playerId.length());
        
        // Проверяем, что после "player_" идут только буквы и цифры
        String suffix = playerId.substring(7); // "player_".length() = 7
        assertTrue(suffix.matches("[a-f0-9]+"));
        assertEquals(12, suffix.length());
    }
}
