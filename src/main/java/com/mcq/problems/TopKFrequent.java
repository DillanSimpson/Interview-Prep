package com.mcq.problems;

import java.util.*;

public class TopKFrequent {
    /**
     * TODO: topKFrequent(nums, k)
     * - Return any order of the k most frequent elements.
     * - Expect O(n log k) with a min-heap or O(n) with bucket sort.
     */
    public int[] topKFrequent(int[] nums, int k) {
        // TODO: implement (heap)
        Map<Integer,Integer> freq = new HashMap<>();
        for (int x: nums) {
            freq.merge(x,1,Integer::sum);
        }
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        for (var e: freq.entrySet()){
            pq.offer(new int[]{e.getKey(), e.getValue()});
            if (pq.size() > k) pq.poll();
        }
        int[] out = new int[k];
        for (int i=k-1;i>=0;i--) {
            out[i] = pq.poll()[0];
        }
        return out;
    }
}
