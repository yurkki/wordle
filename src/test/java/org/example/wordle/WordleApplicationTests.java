package org.example.wordle;

import org.example.wordle.model.GameState;
import org.example.wordle.model.GameStatus;
import org.example.wordle.service.WordleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WordleApplicationTests {

    @Autowired
    private WordleService wordleService;

    @Test
    void contextLoads() {
    }

    @Test
    void testCreateNewGame() {
        GameState game = wordleService.createNewGame();
        assertNotNull(game);
        assertNotNull(game.getTargetWord());
        assertEquals(5, game.getTargetWord().length());
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertEquals(0, game.getGuesses().size());
    }

    @Test
    void testValidWord() {
        assertTrue(wordleService.isValidWord("АВТОР"));
        assertTrue(wordleService.isValidWord("автор"));
        assertFalse(wordleService.isValidWord("ABC"));
        assertFalse(wordleService.isValidWord("АВТО"));
        assertFalse(wordleService.isValidWord("АВТОРР"));
    }

    @Test
    void testProcessGuess() {
        GameState game = new GameState("АВТОР");
        wordleService.processGuess("АВТОР", game);
        
        assertEquals(1, game.getGuesses().size());
        assertEquals(GameStatus.WON, game.getStatus());
        assertTrue(game.isGameOver());
    }

    @Test
    void testProcessWrongGuess() {
        GameState game = new GameState("АВТОР");
        wordleService.processGuess("БАГАЖ", game);
        
        assertEquals(1, game.getGuesses().size());
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertFalse(game.isGameOver());
    }
}
