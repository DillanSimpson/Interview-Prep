package com.mcq.problems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecondLargestTest {

    @Test
    void find() {
        //int[] arr = {23, 55, 67, 45, 76, 14, 52, 98, 29, 59, 40, 36, 98};
        //int[] arr = {12, 35, 1, 10, 34, 1};
        int[] arr = {10, 5, 10};



        int result = SecondLargest.find(arr);

        assertEquals(5, result);
    }
}