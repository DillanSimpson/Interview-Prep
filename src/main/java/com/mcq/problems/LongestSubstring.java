package com.mcq.problems;

import java.util.HashMap;

/**
 * TODO: lengthOfLongestSubstring(s)
 * - Return length of the longest substring without repeating chars.
 * - Use sliding window in O(n) time.
 * Hints: Track last seen index of each char, move left pointer as needed.
 */
public class LongestSubstring {
    public int lengthOfLongestSubstring(String s) {
        // TODO: implement
        var map = new HashMap<Character,Integer>();
        int left = 0, best = 0;
        for (int r=0;r<s.length();r++){
            char c = s.charAt(r);
            if (map.containsKey(c)) left = Math.max(left, map.get(c)+1);
            map.put(c, r);
            best = Math.max(best, r-left+1);
        }
        return best;
    }
}
