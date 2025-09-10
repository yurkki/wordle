package org.example.wordle.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Представляет одну букву в попытке угадать слово
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LetterGuess {
    private char letter;
    private LetterState state;
    
    public LetterGuess(char letter) {
        this.letter = letter;
        this.state = LetterState.ABSENT;
    }
}
