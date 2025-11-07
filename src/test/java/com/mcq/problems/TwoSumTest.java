package com.mcq.problems;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TwoSumTest {
    @Test void example(){
        assertArrayEquals(new int[]{0,1}, new TwoSum().twoSum(new int[]{2,7,11,15}, 9));
    }

    @Test void negatives(){
        int[] out = new TwoSum().twoSum(new int[]{-3,4,3,90}, 0);
        assertArrayEquals(new int[]{0,2}, out);
    }


    /**
     * Map<Integer,Integer> m = new HashMap<>();
     *         for (int i=0;i<nums.length;i++){
     *             int need = target - nums[i];
     *             if (m.containsKey(need)) return new int[]{m.get(need), i};
     *             m.put(nums[i], i);
     *         }
     *         return new int[]{-1,-1};
     *     }
     */
}
