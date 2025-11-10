package com.mcq.problems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MinStackTest {
 @Test
 void happyPath() {
     MinStack minStack = new MinStack();

     minStack.push(-2);
     minStack.push(0);
     minStack.push(-1);
     assertEquals(-2, minStack.getMin());
     assertEquals(-1, minStack.top());
     minStack.pop();
     assertEquals(-2, minStack.getMin());
 }
}