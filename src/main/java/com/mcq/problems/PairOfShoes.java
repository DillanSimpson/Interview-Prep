package com.mcq.problems;

import java.util.HashMap;
import java.util.Map;

/**
 * Given a list of shoes where each shoe is represented as [type, size]:
 *   type 0 = left shoe, type 1 = right shoe
 *   size = shoe size (positive integer)
 *
 * Return true if every shoe can be paired such that each pair contains
 * exactly one left shoe and one right shoe of the same size.
 *
 * Constraints:
 *   - shoes.length is even (odd counts can never fully pair)
 *   - For each distinct size, the count of left shoes must equal the count of right shoes
 *
 * Example:
 *   [[0,21],[1,23],[1,21],[0,23]] → true   (21L+21R, 23L+23R)
 *   [[0,21],[1,23],[1,21],[1,23]] → false  (two right 23s, no left 23)
 */
public class PairOfShoes {

    /**
     * @param shoes 2D array where shoes[i] = [type, size]; type 0=left, 1=right
     * @return true if all shoes can be perfectly paired by type and size
     */
    public boolean pairOfShoes(int[][] shoes) {
        Map<Integer, Integer> map = new HashMap<>();

        for (int[] shoe : shoes) {
                if (shoe[0] == 0) {
                    map.put(shoe[1], map.getOrDefault(shoe[1], 0) + 1);
                } else if (shoe[0] == 1) {
                    map.put(shoe[1], map.getOrDefault(shoe[1], 0) - 1);
                }
            }

        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            if (entry.getValue() != 0) {
                return false;
            }
        }

        return true;
    }
}
