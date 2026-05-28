package com.mcq.problems;

import java.util.HashMap;
import java.util.Map;

/**
 * A blanagram is a pair of same-length strings that can be made into anagrams
 * of each other by substituting EXACTLY ONE character in one of the strings.
 *
 * Two strings are blanagrams if and only if their character frequency maps
 * differ by a total absolute distance of exactly 2 — one character appears
 * one too many times in S1, one character appears one too few times, and
 * all other character counts match.
 *
 * Note: strings that are already anagrams (distance = 0) are NOT blanagrams,
 * since no substitution is made.
 *
 * Example:
 *   "CAT" and "MAP" → frequency diff: C+1, M-1, A=0, T+1, P-1 → distance = 4 → false
 *   "CAT" and "BAT" → frequency diff: C+1, B-1 → distance = 2 → true
 *   "CAT" and "ACT" → already anagrams → distance = 0 → false
 */
public class Blanagram {

    /**
     * Determines whether s1 and s2 are blanagrams.
     *
     * @param s1 first string (non-null)
     * @param s2 second string (non-null)
     * @return true if exactly one character substitution in one string makes them anagrams
     */
    public boolean isBlanagram(String s1, String s2) {
        // TODO: implement
        if (s1.length() != s2.length()) {
            return false;
        }

        Map<Character, Integer> map = new HashMap<>();

        for(char c : s1.toLowerCase().toCharArray()){
            map.put(c, map.getOrDefault(c, 0) + 1);
        }
        for(char c : s2.toLowerCase().toCharArray()){
           //map.merge(c, -1, Integer::sum);
           map.put(c, map.getOrDefault(c, 0) - 1);
        }

        int diff = 0;

        for(Map.Entry<Character, Integer> entry : map.entrySet()){
            if(entry.getValue() > 0){
                diff += entry.getValue();
            }
        }

        return diff == 1;





//        if (s1.length() != s2.length()) {
//            return false;
//        }
//
//        int[] freq = new int[26];
//        for (char c : s1.toLowerCase().toCharArray()){
//            freq[c - 'a']++;
//        }
//        for (char c : s2.toLowerCase().toCharArray()) {
//            freq[c - 'a']--;
//        }
//
//        int surplus = 0;
//        for (int count : freq) {
//            if (count > 0) {
//                surplus += count;
//            }
//        }
//
//        return surplus == 1;
    }
}
