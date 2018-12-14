package radim.outfit

import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Point
import org.junit.Test

import org.junit.Assert.*
import radim.outfit.core.export.work.locusapiextensions.reduceWayPointsSizeTo
import radim.outfit.core.export.work.locusapiextensions.ridUnsupportedRtePtActions

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class CoursepointsUnitTests {
    @Test
    fun testReduceToLimitSingleRteAction() {
        val points = mutableListOf<Point>()
        val limit = 100
        for(j in 0 until 20) {
            points.clear()
            for (i in 0 until limit + j) {
                val p = Point()
                p.parameterRteAction = PointRteAction.PASS_PLACE
                points.add(p)
            }
            println("points size before ${points.size}")
            val reduced = reduceWayPointsSizeTo(points,limit)
            assertEquals(limit, reduced.size)
        }
    }
    @Test
    fun testReduceToLimitMultiRteAction1() {
        val points = mutableListOf<Point>()
        val limit = 100
        repeat(19) {
            val p = Point()
            p.parameterRteAction = PointRteAction.PASS_PLACE
            points.add(p)
        }
        repeat(33){
            val p = Point()
            p.parameterRteAction = PointRteAction.STAY_STRAIGHT
            points.add(p)
        }
        repeat(45){
            val p = Point()
            p.parameterRteAction = PointRteAction.RIGHT_SHARP
            points.add(p)
        }
        repeat(45){
            val p = Point()
            p.parameterRteAction = PointRteAction.LEFT_SHARP
            points.add(p)
        }
        println("points size before ${points.size}")
        val reduced = reduceWayPointsSizeTo(points,limit)
        println("points size after ${reduced.size}")
        assert (reduced.size == 19)
    }
    @Test
    fun testReduceToLimitMultiRteAction2() {
        val points = mutableListOf<Point>()
        val limit = 100
        repeat(3) {
            val p = Point()
            p.parameterRteAction = PointRteAction.PASS_PLACE
            points.add(p)
        }
        repeat(45){
            val p = Point()
            p.parameterRteAction = PointRteAction.RIGHT_SHARP
            points.add(p)
        }
        repeat(45){
            val p = Point()
            p.parameterRteAction = PointRteAction.LEFT_SHARP
            points.add(p)
        }
        repeat(145){
            val p = Point()
            p.parameterRteAction = PointRteAction.STAY_STRAIGHT
            points.add(p)
        }
        println("points size before ${points.size}")
        val reduced = reduceWayPointsSizeTo(points,limit)
        println("points size after ${reduced.size}")
        assert (reduced.size == 93)
    }

    @Test
    fun testRidUnsupported1(){
        val points = mutableListOf<Point>()
        repeat(3) {
            val p = Point()
            p.parameterRteAction = PointRteAction.ROUNDABOUT_EXIT_1
            points.add(p)
        }
        val rid = ridUnsupportedRtePtActions(points)
        assert(rid.size == 0)
    }

    @Test
    fun testRidUnsupported2(){
        val points = mutableListOf<Point>()
        repeat(3) {
            val p = Point()
            p.parameterRteAction = PointRteAction.PASS_PLACE
            points.add(p)
        }
        repeat(3) {
            val p = Point()
            p.parameterRteAction = PointRteAction.ROUNDABOUT_EXIT_1
            points.add(p)
        }
        val rid = ridUnsupportedRtePtActions(points)
        assert(rid.size == 3)
    }
}
