package com.mcq.problems;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ValidParenthesesTest {
    @Test void basic(){
        assertTrue(new ValidParentheses().isValid("()[]{}"));
        assertFalse(new ValidParentheses().isValid("(]"));
        assertFalse(new ValidParentheses().isValid("([)]"));
        assertTrue(new ValidParentheses().isValid("{[]}"));
    }
}
