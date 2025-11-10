package com.mcq.problems;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.mcq.problems.IndexSubarraySum.recursiveBinarySearch;
import static org.junit.jupiter.api.Assertions.*;

class IndexSubarraySumTest {
    @Test
    void example(){
        int[] arr = new int[] {1, 2, 3, 7, 5};
        assertEquals(List.of(2,4), IndexSubarraySum.subarraySum(arr, 12));
    }

    @Test
    void binarySearchTest(){
        int[] arr1 = new int[] {1, 2, 3, 4, 5};
        int[] arr2 = new int[] {11, 22, 33, 44, 55};
        int[] arr3 = new int[] {1, 1, 1, 1, 2};

        assertEquals(3, IndexSubarraySum.binarySearch(arr1, 4));
        assertEquals(-1, IndexSubarraySum.binarySearch(arr2, 445));
        assertEquals(0, IndexSubarraySum.binarySearch(arr3, 1));
    }

    @Test
    void recurBinarySearchTest(){
        int[] arr1 = new int[] {1, 2, 3, 4, 5};
        int[] arr2 = new int[] {11, 22, 33, 44, 55};
        int[] arr3 = new int[] {1, 1, 1, 1, 2};
        int result1 = recursiveBinarySearch(arr1, 0, arr1.length -1, 4);
        int result2 = recursiveBinarySearch(arr2, 0, arr2.length -1, 445);
        int result3 = recursiveBinarySearch(arr3, 0, arr3.length - 1, 1);

        assertEquals(3, result1);
        assertEquals(-1, result2);
        assertEquals(0, result3);
    }
}