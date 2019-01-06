package radim.outfit

import locus.api.objects.enums.PointRteAction
import org.junit.Test

import org.junit.Assert.*
import radim.outfit.core.export.work.locusapiextensions.WaypointSimplified
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
        val points = mutableListOf<WaypointSimplified>()
        val limit = 100
        for(j in 0 until 20) {
            points.clear()
            for (i in 0 until limit + j) {
                val p = WaypointSimplified(-1,"name",
                        PointRteAction.PASS_PLACE, null)
                points.add(p)
            }
            println("points size before ${points.size}")
            val reduced = reduceWayPointsSizeTo(points,limit)
            assertEquals(limit, reduced.size)
        }
    }
    @Test
    fun testReduceToLimitMultiRteAction1() {
        val points = mutableListOf<WaypointSimplified>()
        val limit = 100
        repeat(19) {
            val p = WaypointSimplified(-1,"name",
                    PointRteAction.PASS_PLACE, null)
            points.add(p)
        }
        repeat(33){
            val p = WaypointSimplified(-1,"name",
                    PointRteAction.STAY_STRAIGHT, null)
            points.add(p)
        }
        repeat(45){
            val p = WaypointSimplified(-1,"name",
                    PointRteAction.RIGHT_SHARP, null)
            points.add(p)
        }
        repeat(45){
            val p = WaypointSimplified(-1,"name",
                    PointRteAction.LEFT_SHARP, null)
            points.add(p)
        }
        println("points size before ${points.size}")
        val reduced = reduceWayPointsSizeTo(points,limit)
        println("points size after ${reduced.size}")
        assert (reduced.size == 19)
    }
    @Test
    fun testReduceToLimitMultiRteAction2() {
        val points = mutableListOf<WaypointSimplified>()
        val limit = 100
        repeat(3) {
            val p = WaypointSimplified(-1,"name",
                    PointRteAction.PASS_PLACE, null)
            points.add(p)
        }
        repeat(45){
            val p = WaypointSimplified(-1,"name",
                    PointRteAction.RIGHT_SHARP, null)
            points.add(p)
        }
        repeat(45){
            val p = WaypointSimplified(-1,"name",
                    PointRteAction.LEFT_SHARP, null)
            points.add(p)
        }
        repeat(145){
            val p = WaypointSimplified(-1,"name",
                    PointRteAction.STAY_STRAIGHT, null)
            points.add(p)
        }
        println("points size before ${points.size}")
        val reduced = reduceWayPointsSizeTo(points,limit)
        println("points size after ${reduced.size}")
        assert (reduced.size == 93)
    }

    @Test
    fun testRidUnsupported1(){
        val points = mutableListOf<WaypointSimplified>()
        repeat(3) {
            val p = WaypointSimplified(-1,"name",
                    PointRteAction.ROUNDABOUT_EXIT_1, null)
            points.add(p)
        }
        val rid = ridUnsupportedRtePtActions(points)
        assert(rid.size == 0)
    }

    @Test
    fun testRidUnsupported2(){
        val points = mutableListOf<WaypointSimplified>()
        repeat(3) {
            val p = WaypointSimplified(-1,"name",
                    PointRteAction.PASS_PLACE, null)
            points.add(p)
        }
        repeat(3) {
            val p = WaypointSimplified(-1,"name",
                    PointRteAction.ROUNDABOUT_EXIT_1, null)
            points.add(p)
        }
        val rid = ridUnsupportedRtePtActions(points)
        assert(rid.size == 3)
    }
}
