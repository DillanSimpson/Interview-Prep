package com.mcq.problems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PairOfShoesTest {

    private final PairOfShoes p = new PairOfShoes();

    // --- Provided examples ---

    @Test
    void example_balanced_twoPairs() {
        int[][] shoes = {{0, 21}, {1, 23}, {1, 21}, {0, 23}};
        assertTrue(p.pairOfShoes(shoes));
    }

    @Test
    void example_extraRightShoe_cannotPair() {
        int[][] shoes = {{0, 21}, {1, 23}, {1, 21}, {1, 23}};
        assertFalse(p.pairOfShoes(shoes));
    }

    // --- Edge cases ---

    @Test
    void emptyArray_returnsTrue() {
        assertTrue(p.pairOfShoes(new int[][]{}));
    }

    @Test
    void singlePair_matches() {
        int[][] shoes = {{0, 10}, {1, 10}};
        assertTrue(p.pairOfShoes(shoes));
    }

    @Test
    void singlePair_sizeMismatch() {
        int[][] shoes = {{0, 10}, {1, 11}};
        assertFalse(p.pairOfShoes(shoes));
    }

    // --- Type imbalance ---

    @Test
    void twoLeftShoes_sameSize_cannotPair() {
        int[][] shoes = {{0, 9}, {0, 9}};
        assertFalse(p.pairOfShoes(shoes));
    }

    @Test
    void twoRightShoes_sameSize_cannotPair() {
        int[][] shoes = {{1, 9}, {1, 9}};
        assertFalse(p.pairOfShoes(shoes));
    }

    // --- Multiple sizes ---

    @Test
    void multipleSizes_allBalanced() {
        int[][] shoes = {{0, 7}, {1, 7}, {0, 8}, {1, 8}, {0, 9}, {1, 9}};
        assertTrue(p.pairOfShoes(shoes));
    }

    @Test
    void multipleSizes_oneSizeUnbalanced() {
        // size 8 has two lefts, one right
        int[][] shoes = {{0, 7}, {1, 7}, {0, 8}, {0, 8}, {1, 8}};
        assertFalse(p.pairOfShoes(shoes));
    }

    @Test
    void duplicatePairs_sameSize_balanced() {
        // Two pairs of size 10
        int[][] shoes = {{0, 10}, {1, 10}, {0, 10}, {1, 10}};
        assertTrue(p.pairOfShoes(shoes));
    }

    @Test
    void duplicatePairs_sameSize_extraLeft() {
        // Three lefts, one right of size 10
        int[][] shoes = {{0, 10}, {0, 10}, {0, 10}, {1, 10}};
        assertFalse(p.pairOfShoes(shoes));
    }

    // --- Order independence ---

    @Test
    void shuffledOrder_stillPairs() {
        int[][] shoes = {{1, 12}, {0, 9}, {1, 9}, {0, 12}};
        assertTrue(p.pairOfShoes(shoes));
    }
}
