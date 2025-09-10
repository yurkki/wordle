package org.example.wordle.model;

/**
 * Состояние буквы в слове
 */
public enum LetterState {
    CORRECT,    // Буква на правильном месте
    PRESENT,    // Буква есть в слове, но на другом месте
    ABSENT      // Буквы нет в слове
}
