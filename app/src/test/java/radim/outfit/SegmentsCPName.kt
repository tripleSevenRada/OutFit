package radim.outfit

import com.garmin.fit.CoursePoint
import org.junit.Test

class SegmentsCPName {
    @Test
    fun printStartFinishNames(){
        System.out.println(CoursePoint.SEGMENT_START.name)
        System.out.println(CoursePoint.SEGMENT_END.name)
    }
}