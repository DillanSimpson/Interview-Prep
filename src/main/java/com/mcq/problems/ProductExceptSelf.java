package com.mcq.problems;

public class ProductExceptSelf {
    /**
     * TODO: productExceptSelf(nums)
     * - Return array where result[i] = product of all nums[j] for j!=i.
     * - No division; O(n) time; O(1) extra space (excluding output).
     */
    public int[] productExceptSelf(int[] nums) {
        // TODO: implement prefix/suffix
        int n = nums.length;
        int[] out = new int[n];
        int pref = 1;
        for (int i=0;i<n;i++){ out[i] = pref; pref *= nums[i]; }
        int suff = 1;
        for (int i=n-1;i>=0;i--){ out[i] *= suff; suff *= nums[i]; }
        return out;
    }
}
