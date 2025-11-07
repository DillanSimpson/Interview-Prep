package com.mcq.problems;

import java.util.ArrayDeque;
import java.util.Queue;

public class RottingOranges {
    /**
     * TODO: orangesRotting(grid)
     * - 0 empty, 1 fresh, 2 rotten.
     * - Each minute, rotten spreads to 4-neighbors.
     * - Return minutes until all rotten, or -1 if impossible.
     * Approach: multi-source BFS from all rotten cells, count fresh.
     */
    public int orangesRotting(int[][] g) {
        int m=g.length, n=g[0].length;
        Queue<int[]> q = new ArrayDeque<>();
        int fresh=0;
        for (int i=0;i<m;i++) for (int j=0;j<n;j++){
            if (g[i][j]==2) q.offer(new int[]{i,j});
            else if (g[i][j]==1) fresh++;
        }
        if (fresh==0) return 0;
        int minutes=-1;
        int[][] dirs={{1,0},{-1,0},{0,1},{0,-1}};
        while(!q.isEmpty()){
            int sz=q.size(); minutes++;
            for (int k=0;k<sz;k++){
                var cur=q.poll();
                for (var d: dirs){
                    int ni=cur[0]+d[0], nj=cur[1]+d[1];
                    if (ni<0||nj<0||ni>=m||nj>=n||g[ni][nj]!=1) continue;
                    g[ni][nj]=2; fresh--; q.offer(new int[]{ni,nj});
                }
            }
        }
        return fresh==0? minutes : -1;
    }
}
