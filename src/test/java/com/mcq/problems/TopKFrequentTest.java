package com.mcq.problems;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

public class TopKFrequentTest {
    @Test void sample(){
        int[] out = new TopKFrequent().topKFrequent(new int[]{1,1,1,2,2,3}, 2);
        Arrays.sort(out);
        assertArrayEquals(new int[]{1,2}, out);
    }
    @Test void single(){
        int[] out = new TopKFrequent().topKFrequent(new int[]{4,4,4,5}, 1);
        assertEquals(4, out[0]);
    }
}
