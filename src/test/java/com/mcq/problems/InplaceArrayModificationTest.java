package com.mcq.problems;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class InplaceArrayModificationTest {
    @Test
    void testNormalization() {
        int[] scores = {120, -5, 90, 0, 45, 110, 0, -1};
        InplaceArrayModification.normalize(scores);
        assertArrayEquals(new int[]{100, 90, 45, 100, -1, -1, -1, -1}, scores);
    }

    @Test
    void testAllInvalid() {
        int[] scores = {-10, -20, 0};
        InplaceArrayModification.normalize(scores);
        assertArrayEquals(new int[]{-1, -1, -1}, scores);
    }

    @Test
    void testNoChange() {
        int[] scores = {50, 60, 70};
        InplaceArrayModification.normalize(scores);
        assertArrayEquals(new int[]{50, 60, 70}, scores);
    }
}
