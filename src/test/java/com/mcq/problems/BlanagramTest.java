package com.mcq.problems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlanagramTest {

    private final Blanagram b = new Blanagram();

    // --- True cases ---

    @Test
    void oneCharDifferent_isBlanagram() {
        // "CAT" → change C to B → "BAT", which is an anagram of "BAT"
        assertTrue(b.isBlanagram("CAT", "BAT"));
    }

    @Test
    void substituteLastChar() {
        assertTrue(b.isBlanagram("MAPS", "MAST"));
    }

    @Test
    void substituteFirstChar() {
        assertTrue(b.isBlanagram("ZOPE", "ROPE"));
    }

    @Test
    void singleCharStrings_differentChars_isBlanagram() {
        // "A" → substitute A with B → "B" is an anagram of "B"
        assertTrue(b.isBlanagram("A", "B"));
    }

    @Test
    void orderDoesNotMatter_swappedArgs() {
        assertTrue(b.isBlanagram("BAT", "CAT"));
    }

    // --- False cases ---

    @Test
    void alreadyAnagrams_notBlanagram() {
        // No substitution needed — exactly 0 changes, not 1
        assertFalse(b.isBlanagram("CAT", "ACT"));
    }

    @Test
    void identicalStrings_notBlanagram() {
        assertFalse(b.isBlanagram("CAT", "CAT"));
    }

    @Test
    void twoDifferences_notBlanagram() {
        // "CAT" vs "MAP": C↔M and T↔P differ → distance = 4
        assertFalse(b.isBlanagram("CAT", "MAP"));
    }

    @Test
    void differentLengths_notBlanagram() {
        assertFalse(b.isBlanagram("CAT", "CATS"));
    }

    @Test
    void emptyStrings_notBlanagram() {
        // Both empty → already "anagrams" with 0 substitutions
        assertFalse(b.isBlanagram("", ""));
    }

    @Test
    void singleCharStrings_sameChar_notBlanagram() {
        assertFalse(b.isBlanagram("A", "A"));
    }

    @Test
    void completelyDifferentStrings_notBlanagram() {
        assertFalse(b.isBlanagram("ABCDE", "VWXYZ"));
    }

    @Test
    void duplicateCharacters_blanagram() {
        // "AABB" → change one A to C → "CABB", anagram of "ABBC"... let's use a clean case
        // "AABB" vs "AACB": B surplus, C deficit → distance = 2 → true
        assertTrue(b.isBlanagram("AABB", "AACB"));
    }

    @Test
    void duplicateCharacters_blanagram2() {
        assertFalse(b.isBlanagram("AABB", "AACD"));
    }

    @Test
    void duplicateCharacters_alreadyAnagram_notBlanagram() {
        assertFalse(b.isBlanagram("AABB", "BBAA"));
    }
}
