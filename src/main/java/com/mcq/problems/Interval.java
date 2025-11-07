package com.mcq.problems;

public class Interval {
    public int start;
    public int end;
    public Interval(int s, int e) { this.start = s; this.end = e; }
    @Override public String toString() { return "["+start+","+end+"]"; }
    @Override public boolean equals(Object o){
        if(!(o instanceof Interval i)) return false;
        return i.start==start && i.end==end;
    }
    @Override public int hashCode(){ return start*31+end; }
}
