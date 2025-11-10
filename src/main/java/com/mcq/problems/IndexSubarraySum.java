package com.mcq.problems;

import java.util.ArrayList;
import java.util.List;


/*
 * Given an array arr[] containing only non-negative integers,
 * your task is to find a continuous subarray (a contiguous sequence of elements) whose sum equals a specified value target.
 * You need to return the 1-based indices of the leftmost and rightmost elements of this subarray.
 * You need to find the first subarray whose sum is equal to the target.
 * Note: If no such array is possible then, return [-1].
 */

/**
 * TODO: lengthOfLongestSubstring(s)
 * Input: arr[] = [1, 2, 3, 7, 5], target = 12
 * Output: [2, 4]
 * Explanation: The sum of elements from 2nd to 4th position is 12.
 */
public class IndexSubarraySum {
    static ArrayList<Integer> subarraySum(int[] arr, int target) {
        // code here
        int sum;
        ArrayList<Integer> ans = new ArrayList<>();

        for(int i = 0; i < arr.length; i++) {
            if(arr[i] == target) {
                ans.add(i+1);
                continue;
            }
            sum = arr[i];
            for(int j = i + 1; j < arr.length; j++) {
                sum += arr[j];
                if(sum > target) {
                    break;
                } else if(sum == target) {
                    ans.add(i+1);
                    ans.add(j+1);
                    return ans;
                }
            }
        }

        ans.add(-1);
        return ans;
    }


    static int binarySearch(int[] arr, int x) {
        int low = 0;
        int high = arr.length - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;

            // Check if x is present at mid
            if (arr[mid] == x)
                return mid;

            // If x greater, ignore left half
            if (arr[mid] < x)
                low = mid + 1;

                // If x is smaller, ignore right half
            else
                high = mid - 1;
        }

        // If we reach here, then element was
        // not present
        return -1;

//        for(int i = 0; i < arr.length; i++) {
//            if(arr[i] == k) {
//                return i;
//            }
//        }

       // return -1;
    }


    // A recursive binary search function. It returns
    // location of x in given array arr[low..high] is present,
    // otherwise -1
    static int recursiveBinarySearch(int[] arr, int low, int high, int x) {
        if (high >= low) {
            if(x == arr[low])
            {
                return low;
            }
            int mid = low + (high - low) / 2;

            // If the element is present at the
            // middle itself
            if (arr[mid] == x)
                return mid;

            // If element is smaller than mid, then
            // it can only be present in left subarray
            if (arr[mid] > x)
                return recursiveBinarySearch(arr, low, mid - 1, x);

            // Else the element can only be present
            // in right subarray
            return recursiveBinarySearch(arr, mid + 1, high, x);
        }

        // We reach here when element is not present
        // in array
        return -1;
    }


}
