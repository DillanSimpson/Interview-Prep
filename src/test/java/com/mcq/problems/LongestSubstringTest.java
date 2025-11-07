package com.mcq.problems;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LongestSubstringTest {
    @Test void samples(){
        assertEquals(3, new LongestSubstring().lengthOfLongestSubstring("abcabcbb"));
        assertEquals(1, new LongestSubstring().lengthOfLongestSubstring("bbbbb"));
        assertEquals(3, new LongestSubstring().lengthOfLongestSubstring("pwwkew"));
        assertEquals(0, new LongestSubstring().lengthOfLongestSubstring(""));
    }
}
