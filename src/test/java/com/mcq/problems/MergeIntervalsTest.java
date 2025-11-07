package com.mcq.problems;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class MergeIntervalsTest {
    @Test void example(){
        var in = List.of(new Interval(1,3), new Interval(2,6), new Interval(8,10), new Interval(15,18));
        var out = new MergeIntervals().merge(in);
        assertEquals(List.of(new Interval(1,6), new Interval(8,10), new Interval(15,18)), out);
    }
    @Test void touch(){
        var in = List.of(new Interval(1,4), new Interval(4,5));
        var out = new MergeIntervals().merge(in);
        assertEquals(List.of(new Interval(1,5)), out);
    }
}
