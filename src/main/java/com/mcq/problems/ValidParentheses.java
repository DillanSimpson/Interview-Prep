package com.mcq.problems;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * TODO: isValid(s)
 * - Return true if brackets are valid and properly nested: ()[]{}.
 * - Empty string is valid.
 * - Time O(n), Space O(n).
 * Hints: Use a stack and a map of close->open.
 */
public class ValidParentheses {
    public boolean isValid(String s) {
        // TODO: implement
        Map<Character,Character> pairs = Map.of(')', '(', ']', '[', '}', '{');
        Deque<Character> st = new ArrayDeque<>();
        for (char c: s.toCharArray()){
            if (pairs.containsValue(c)) st.push(c);
            else if (pairs.containsKey(c)) {
                if (st.isEmpty() || st.pop()!=pairs.get(c)) return false;
            } else return false;
        }
        return st.isEmpty();
    }
}
