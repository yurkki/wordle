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
