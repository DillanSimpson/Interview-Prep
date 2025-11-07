package com.mcq.problems;

import java.util.*;

/**
 * TODO: Implement twoSum(nums, target)
 * - Return indices (i, j) such that nums[i] + nums[j] == target, i < j
 * - Exactly one solution exists, all nums may be negative/duplicate.
 * - Time: O(n), Space: O(n).
 * Hints: Use a hash map from value -> index; check complement (target - nums[i]).
 */
public class TwoSum {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();

        for(int i = 0; i < nums.length; i++){
            int need = target - nums[i];
            if(map.containsKey(need)){
                return new int[]{map.get(need), i};
            }
            map.put(nums[i], i);
        }
        return new int[0];
    }
}
