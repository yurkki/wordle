package org.example.wordle.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Состояние игры Wordle
 */
@Data
public class GameState {
    private String targetWord;
    private List<WordGuess> guesses;
    private GameStatus status;
    private GameMode gameMode;
    private String playerId;
    private int gameTimeSeconds;
    
    public GameState() {
        this.guesses = new ArrayList<>();
        this.status = GameStatus.IN_PROGRESS;
        this.gameMode = GameMode.GUESS;
    }
    
    public GameState(String targetWord) {
        this();
        this.targetWord = targetWord.toUpperCase();
    }
    
    public GameState(String targetWord, GameMode gameMode) {
        this();
        this.targetWord = targetWord.toUpperCase();
        this.gameMode = gameMode;
    }
    
    public void addGuess(WordGuess guess) {
        guesses.add(guess);
    }
    
    public boolean isGameOver() {
        return status != GameStatus.IN_PROGRESS;
    }
    
    public boolean canMakeGuess() {
        return guesses.size() < 6 && status == GameStatus.IN_PROGRESS;
    }
}
