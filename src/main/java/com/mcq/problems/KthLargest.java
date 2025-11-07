package com.mcq.problems;

import java.util.PriorityQueue;

public class KthLargest {
    /**
     * TODO: Design KthLargest(k, nums) supporting add(val)->kth largest.
     * - Use a min-heap of size k.
     * - Time O(log k) per add.
     */
    private final int k;
    private final PriorityQueue<Integer> pq = new PriorityQueue<>();

    public KthLargest(int k, int[] nums) {
        this.k = k;
        for (int x: nums) add(x);
    }

    public int add(int val) {
        pq.offer(val);
        if (pq.size() > k) pq.poll();
        return pq.peek();
    }
}
