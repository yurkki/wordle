package org.example.wordle.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Представляет одну попытку угадать слово
 */
@Data
public class WordGuess {
    private List<LetterGuess> letters;
    private String word;
    
    public WordGuess(String word) {
        this.word = word.toUpperCase();
        this.letters = new ArrayList<>();
        for (char c : this.word.toCharArray()) {
            this.letters.add(new LetterGuess(c));
        }
    }
    
    public boolean isComplete() {
        return letters.size() == 5;
    }
}
