package com.mcq.problems;

public class SecondLargest {

    static int find(int[] arr) {
        int largest = 0;
        int secondLargest = -1;

        for(int i : arr) {
            if (i > largest) {
                secondLargest = largest;
                largest = i;
            } else if (i > secondLargest && i != largest) {
                secondLargest = i;
            }
        }
        return secondLargest;
    }


}