package com.mcq.problems;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LRUCacheTest {
    @Test void behavior(){
        LRUCache lru = new LRUCache(2);
        lru.put(1,1);
        lru.put(2,2);
        assertEquals(1, lru.get(1));
        lru.put(3,3); // evicts key 2
        assertEquals(-1, lru.get(2));
        lru.put(4,4); // evicts key 1
        assertEquals(-1, lru.get(1));
        assertEquals(3, lru.get(3));
        assertEquals(4, lru.get(4));
    }
}
