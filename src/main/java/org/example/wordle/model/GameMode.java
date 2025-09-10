package org.example.wordle.model;

/**
 * Режимы игры Wordle
 */
public enum GameMode {
    GUESS("Угадывать"),
    DAILY("Слово дня");
    
    private final String displayName;
    
    GameMode(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
