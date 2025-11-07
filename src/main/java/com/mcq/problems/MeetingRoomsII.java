package com.mcq.problems;

import java.util.Arrays;
import java.util.PriorityQueue;

public class MeetingRoomsII {
    /**
     * TODO: minMeetingRooms(intervals)
     * - Return minimum rooms required to hold all meetings.
     * - Greedy: sort by start time; use min-heap of end times.
     */
    public int minMeetingRooms(int[][] intervals) {
        // TODO: implement
        if (intervals.length==0) return 0;
        Arrays.sort(intervals, (a,b)->Integer.compare(a[0], b[0]));
        PriorityQueue<Integer> ends = new PriorityQueue<>();
        for (int[] it: intervals){
            if (!ends.isEmpty() && ends.peek() <= it[0]) ends.poll();
            ends.offer(it[1]);
        }
        return ends.size();
    }
}
