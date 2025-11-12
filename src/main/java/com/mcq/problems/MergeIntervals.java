package com.mcq.problems;

import java.util.*;

public class MergeIntervals {
    /**
     * TODO: merge(intervals)
     * - Given list of [start,end], merge overlapping intervals.
     * - Return sorted, non-overlapping list.
     * - Time O(n log n) for sorting, Space O(n).
     * Hints: sort by start, sweep and extend end as needed.
     */
    public List<Interval> merge(List<Interval> intervals) {
        // TODO: implement
        if (intervals.isEmpty()) {
            return List.of();
        }

        intervals = new ArrayList<>(intervals);
        intervals.sort(Comparator.comparingInt(i -> i.start));

        List<Interval> out = new ArrayList<>();
        int s = intervals.get(0).start;
        int e = intervals.get(0).end;

        for (int i=1;i<intervals.size();i++){
            Interval cur = intervals.get(i);
            if (cur.start <= e) {
                e = Math.max(e, cur.end);
            }
            else {
                out.add(new Interval(s,e));
                s = cur.start;
                e = cur.end;
            }
        }
        out.add(new Interval(s,e));
        return out;
    }
}
