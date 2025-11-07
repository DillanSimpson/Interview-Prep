package com.mcq.problems;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ProductExceptSelfTest {
    @Test void sample(){
        int[] out = new ProductExceptSelf().productExceptSelf(new int[]{1,2,3,4});
        assertArrayEquals(new int[]{24,12,8,6}, out);
    }
    @Test void zeros(){
        int[] out = new ProductExceptSelf().productExceptSelf(new int[]{-1,1,0,-3,3});
        assertArrayEquals(new int[]{0,0,9,0,0}, out);
    }
}
