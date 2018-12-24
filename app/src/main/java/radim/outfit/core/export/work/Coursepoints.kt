package radim.outfit.core.export.work

import android.util.Log
import com.garmin.fit.CoursePoint
import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Point
import locus.api.objects.extra.Track

const val COURSEPOINTS_LIMIT = 100

val routePointActionsToCoursePoints: Map<PointRteAction, CoursePoint> = mapOf(
        PointRteAction.LEFT_SLIGHT to CoursePoint.SLIGHT_LEFT,
        PointRteAction.RAMP_ON_LEFT to CoursePoint.LEFT_FORK,
        PointRteAction.MERGE_LEFT to CoursePoint.SLIGHT_LEFT,
        PointRteAction.LEFT to CoursePoint.LEFT,
        PointRteAction.LEFT_SHARP to CoursePoint.SHARP_LEFT,
        PointRteAction.U_TURN_LEFT to CoursePoint.SHARP_LEFT,
        PointRteAction.EXIT_LEFT to CoursePoint.LEFT,
        PointRteAction.RIGHT_SLIGHT to CoursePoint.SLIGHT_RIGHT,
        PointRteAction.RAMP_ON_RIGHT to CoursePoint.RIGHT_FORK,
        PointRteAction.MERGE_RIGHT to CoursePoint.SLIGHT_RIGHT,
        PointRteAction.RIGHT to CoursePoint.RIGHT,
        PointRteAction.RIGHT_SHARP to CoursePoint.SHARP_RIGHT,
        PointRteAction.U_TURN_RIGHT to CoursePoint.SHARP_RIGHT,
        PointRteAction.EXIT_RIGHT to CoursePoint.RIGHT,
        PointRteAction.U_TURN to CoursePoint.U_TURN,
        PointRteAction.CONTINUE_STRAIGHT to CoursePoint.STRAIGHT,
        PointRteAction.STAY_STRAIGHT to CoursePoint.STRAIGHT,
        PointRteAction.RAMP_STRAIGHT to CoursePoint.MIDDLE_FORK,
        PointRteAction.MERGE to CoursePoint.STRAIGHT,
        PointRteAction.PASS_PLACE to CoursePoint.GENERIC
)

val coursePointsDisplayOrder: List<CoursePoint> = listOf(
        CoursePoint.GENERIC,
        CoursePoint.SUMMIT,
        CoursePoint.VALLEY,
        CoursePoint.WATER,
        CoursePoint.FOOD,
        CoursePoint.DANGER,
        CoursePoint.LEFT,
        CoursePoint.RIGHT,
        CoursePoint.STRAIGHT,
        CoursePoint.FIRST_AID,
        CoursePoint.FOURTH_CATEGORY,
        CoursePoint.THIRD_CATEGORY,
        CoursePoint.SECOND_CATEGORY,
        CoursePoint.FIRST_CATEGORY,
        CoursePoint.HORS_CATEGORY,
        CoursePoint.SPRINT,
        CoursePoint.LEFT_FORK,
        CoursePoint.RIGHT_FORK,
        CoursePoint.MIDDLE_FORK,
        CoursePoint.SLIGHT_LEFT,
        CoursePoint.SHARP_LEFT,
        CoursePoint.SLIGHT_RIGHT,
        CoursePoint.SHARP_RIGHT,
        CoursePoint.U_TURN,
        CoursePoint.SEGMENT_START,
        CoursePoint.SEGMENT_END,
        CoursePoint.INVALID
)

// low value low priority
val routePointActionsPrioritized: Map<Int, List<PointRteAction>> = mapOf(
        1 to listOf(PointRteAction.MERGE),
        2 to listOf(PointRteAction.RAMP_STRAIGHT),
        3 to listOf(PointRteAction.STAY_STRAIGHT),
        4 to listOf(PointRteAction.CONTINUE_STRAIGHT),
        5 to listOf(PointRteAction.RAMP_ON_LEFT, PointRteAction.RAMP_ON_RIGHT),
        6 to listOf(PointRteAction.MERGE_LEFT, PointRteAction.MERGE_RIGHT),
        7 to listOf(PointRteAction.LEFT_SLIGHT, PointRteAction.RIGHT_SLIGHT),
        8 to listOf(PointRteAction.EXIT_LEFT, PointRteAction.EXIT_RIGHT),
        9 to listOf(PointRteAction.U_TURN_LEFT, PointRteAction.U_TURN_RIGHT),
        10 to listOf(PointRteAction.U_TURN),
        11 to listOf(PointRteAction.RIGHT_SHARP, PointRteAction.LEFT_SHARP),
        12 to listOf(PointRteAction.RIGHT, PointRteAction.LEFT),
        13 to listOf(PointRteAction.PASS_PLACE)
)

const val MAX_DISTANCE_TO_CLIP_WP_TO_COURSE = 200.0F

class AttachWaypointsToTrack(val track: Track) {

    // try to assign (trackpoints) indices to waypoints with PointRteAction.UNDEFINED
    // that lay close enough to track

    var debugMesseges = mutableListOf<String>()
        private set
    private var tag = "AttachWaypoints"

    fun rebuild(wpts: List<Point>, debug: Boolean): List<WaypointSimplified> {

        val waypoints = wpts.toMutableList() // IS POINT
        val indicesTaken = mutableSetOf<Int>()
        val waypointsUndefined = mutableListOf<Point>() // IS POINT
        val waypointsSimplified = mutableListOf<WaypointSimplified>()
        val waypointsToListOfIndices = mutableMapOf<Point, List<Int>>()

        if (debug) debugMesseges.add("Size of all waypoints received in rebuild() ${waypoints.size}")

        // move all PointRteAction.UNDEFINED waypoints to waypointsUndefined
        waypoints.forEach {
            if (it.parameterRteAction == PointRteAction.UNDEFINED)
                waypointsUndefined.add(it)
        }
        waypointsUndefined.forEach { waypoints.remove(it) }
        
        // copy waypoints to waypointsSimplified
        waypoints.forEach {
            if (it.parameterStyleName == null) it.parameterStyleName = "poi"
            waypointsSimplified.add(WaypointSimplified(it))
        }

        if (debug) debugMesseges.add("Size of waypoints after UNDEFINED removed ${waypoints.size}")
        if (debug) debugMesseges.add("Size of waypointsUndefined removed from waypoints ${waypointsUndefined.size}")

        // mark trackpoints indices of mutableWaypoints as taken
        waypoints.forEach {
            if (it.paramRteIndex == -1) {
                Log.w(tag, "rebuild - unexpected -1 from it.paramRteIndex")
                if (debug) debugMesseges.add("rebuild - unexpected -1 from it.paramRteIndex")

            }
            indicesTaken.add(it.paramRteIndex)
        }

        // now try to attach waypointsUndefined to not yet taken route indices
        waypointsUndefined.forEach {
            waypointsToListOfIndices.put(it, getSortedListOfIndicesCloseEnough(it))
        }

        waypointsToListOfIndices.keys.forEach {
            val waypoint = it
            val listIndicesCloseEnough = waypointsToListOfIndices[waypoint]
            var indexAssigned = false
            listIndicesCloseEnough?.forEach {
                if (!indexAssigned &&
                        track.points[it] != null &&
                        !indicesTaken.contains(it)) {
                    indicesTaken.add(it)
                    // build new simplified waypoint, add it to attachedWaypoints
                    val attachedWaypoint = WaypointSimplified(
                            it,
                            waypoint.parameterStyleName ?: "poi",
                            PointRteAction.PASS_PLACE
                    )
                    waypointsSimplified.add(attachedWaypoint)
                    if (debug) debugMesseges.add("Attached new simplified waypoint: $attachedWaypoint")
                    indexAssigned = true
                }
            }
        }
        waypointsSimplified.sort()
        if (debug) debugMesseges.add("Sorted rebuilt simplified waypoints")
        if (debug) {
            waypointsSimplified.forEach { debugMesseges.add(it.toString()) }
        }
        return waypointsSimplified // rebuild
    }

    private fun getSortedListOfIndicesCloseEnough(waypoint: Point): List<Int> {
        val list = mutableListOf<Int>()
        val listDistInd = mutableListOf<IndexDistance>()
        var dist: Float
        for (i in track.points.indices) {
            if (track.points[i] == null) continue
            dist = waypoint.location.distanceTo(track.points[i])
            if (dist < MAX_DISTANCE_TO_CLIP_WP_TO_COURSE)
                listDistInd.add(IndexDistance(i, dist))
        }
        with(listDistInd) { sort(); forEach { list.add(it.index) } }
        return list
    }
}

data class WaypointSimplified(val rteIndex: Int,
                              val styleName: String,
                              val rteAction: PointRteAction) : Comparable<WaypointSimplified> {
    constructor(point: Point) : this(point.paramRteIndex, point.parameterStyleName, point.parameterRteAction)

    override fun compareTo(other: WaypointSimplified): Int = this.rteIndex.compareTo(other.rteIndex)
}

data class IndexDistance(val index: Int, val distance: Float) : Comparable<IndexDistance> {
    override fun compareTo(other: IndexDistance): Int = this.distance.compareTo(other.distance)
}
