package com.mcq.problems;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RottingOrangesTest {
    @Test void sample(){
        int[][] g = {{2,1,1},{1,1,0},{0,1,1}};
        assertEquals(4, new RottingOranges().orangesRotting(g));
    }
    @Test void impossible(){
        int[][] g = {{2,1,1},{0,1,1},{1,0,1}};
        assertEquals(-1, new RottingOranges().orangesRotting(g));
    }
}
