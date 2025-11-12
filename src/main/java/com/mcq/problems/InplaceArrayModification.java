package com.mcq.problems;

public class InplaceArrayModification {

    /**
     * Normalize transaction scores in place.
     * Steps:
     *  1) Clamp: <0 -> 0, >100 -> 100
     *  2) Remove zeros by shifting non-zero values left
     *  3) Fill remaining tail with -1
     *
     * Time:  O(n)
     * Space: O(1)
     */
    public static void normalize ( int[] scores){
        if (scores == null || scores.length == 0) return;

        int write = 0; // next position to write a kept (non-zero) value

        // Single pass: clamp and compact non-zeros
        for (int i = 0; i < scores.length; i++) {
            int v = scores[i];
            if (v < 0) {
                v = 0;
            } else if (v > 100) {
                v = 100;
            }

            if (v != 0) {
                scores[write++] = v;
            }
        }

        // Fill the remainder with -1
        while (write < scores.length) {
            scores[write++] = -1;
        }
    }
}
