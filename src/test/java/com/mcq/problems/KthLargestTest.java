package com.mcq.problems;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class KthLargestTest {
    @Test void example(){
        KthLargest kth = new KthLargest(3, new int[]{4,5,8,2});
        assertEquals(4, kth.add(3));
        assertEquals(5, kth.add(5));
        assertEquals(5, kth.add(10));
        assertEquals(8, kth.add(9));
        assertEquals(8, kth.add(4));
    }
}
