package com.mcq.problems;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TODO: Implement LRUCache with O(1) get/put and eviction on capacity overflow.
 * - Constructor LRUCache(int capacity)
 * - int get(int key): returns -1 if missing
 * - void put(int key, int value)
 * Hints: Use LinkedHashMap with accessOrder=true.
 */
public class LRUCache {
    private final LinkedHashMap<Integer,Integer> map;

    public LRUCache(int capacity) {
        // TODO: implement with accessOrder=true
        this.map = new LinkedHashMap<>(16, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<Integer,Integer> eldest) {
                return size() > capacity;
            }
        };
    }

    public int get(int key) { return map.getOrDefault(key, -1); }

    public void put(int key, int value) { map.put(key, value); }
}
