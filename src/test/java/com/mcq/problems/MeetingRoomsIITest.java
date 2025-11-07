package com.mcq.problems;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MeetingRoomsIITest {
    @Test void sample(){
        int[][] intervals = {{0,30},{5,10},{15,20}};
        assertEquals(2, new MeetingRoomsII().minMeetingRooms(intervals));
        assertEquals(1, new MeetingRoomsII().minMeetingRooms(new int[][]{{7,10},{2,4}}));
    }
}
