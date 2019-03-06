package radim.outfit.core.export.work

import android.util.Log
import com.garmin.fit.CoursePoint
import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Point
import radim.outfit.core.export.work.locusapiextensions.TrackContainer
import radim.outfit.core.export.work.locusapiextensions.WaypointSimplified
import radim.outfit.core.export.work.locusapiextensions.getCoursepointEnumForced
import radim.outfit.core.export.work.locusapiextensions.getWaypointName
import radim.outfit.core.export.work.locusapiextensions.stringdumps.LocationStringDump
import java.lang.RuntimeException

const val COURSEPOINTS_LIMIT = 100
const val COURSEPOINTS_NAME_MAX_LENGTH = 20
const val MAX_DISTANCE_TO_CLIP_WP_TO_COURSE = 260.0F

/*
   LOCUS API

   UNDEFINED(Integer.MIN_VALUE, "undefined"),
   /**
    * No maneuver occurs here.
    */
   NO_MANEUVER(0, "no_maneuver"),
   /**
    * Continue straight.
    */
   CONTINUE_STRAIGHT(1, "straight"),
   /**
    * No maneuver occurs here. Road name changes.
    */
   NO_MANEUVER_NAME_CHANGE(2, "name_change"),
   /**
    * Make a slight left.
    */
   LEFT_SLIGHT(3, "left_slight"),
   /**
    * Turn left.
    */
   LEFT(4, "left"),
   /**
    * Make a sharp left.
    */
   LEFT_SHARP(5, "left_sharp"),
   /**
    * Make a slight right.
    */
   RIGHT_SLIGHT(6, "right_slight"),
   /**
    * Turn right.
    */
   RIGHT(7, "right"),
   /**
    * Make a sharp right.
    */
   RIGHT_SHARP(8, "right_sharp"),
   /**
    * Stay left.
    */
   STAY_LEFT(9, "stay_left"),
   /**
    * Stay right.
    */
   STAY_RIGHT(10, "stay_right"),
   /**
    * Stay straight.
    */
   STAY_STRAIGHT(11, "stay_straight"),
   /**
    * Make a U-turn.
    */
   U_TURN(12, "u-turn"),
   /**
    * Make a left U-turn.
    */
   U_TURN_LEFT(13, "u-turn_left"),
   /**
    * Make a right U-turn.
    */
   U_TURN_RIGHT(14, "u-turn_right"),
   /**
    * Exit left.
    */
   EXIT_LEFT(15, "exit_left"),
   /**
    * Exit right.
    */
   EXIT_RIGHT(16, "exit_right"),
   /**
    * Take the ramp on the left.
    */
   RAMP_ON_LEFT(17, "ramp_left"),
   /**
    * Take the ramp on the right.
    */
   RAMP_ON_RIGHT(18, "ramp_right"),
   /**
    * Take the ramp straight ahead.
    */
   RAMP_STRAIGHT(19, "ramp_straight"),
   /**
    * Merge left.
    */
   MERGE_LEFT(20, "merge_left"),
   /**
    * Merge right.
    */
   MERGE_RIGHT(21, "merge_right"),
   /**
    * Merge.
    */
   MERGE(22, "merge"),
   /**
    * Enter state/province.
    */
   ENTER_STATE(23, "enter_state"),
   /**
    * Arrive at your destination.
    */
   ARRIVE_DEST(24, "dest"),
   /**
    * Arrive at your destination on the left.
    */
   ARRIVE_DEST_LEFT(25, "dest_left"),
   /**
    * Arrive at your destination on the right.
    */
   ARRIVE_DEST_RIGHT(26, "dest_right"),
   /**
    * Enter the roundabout and take the 1st exit.
    */
   ROUNDABOUT_EXIT_1(27, "roundabout_e1"),
   /**
    * Enter the roundabout and take the 2nd exit.
    */
   ROUNDABOUT_EXIT_2(28, "roundabout_e2"),
   /**
    * Enter the roundabout and take the 3rd exit.
    */
   ROUNDABOUT_EXIT_3(29, "roundabout_e3"),
   /**
    * Enter the roundabout and take the 4th exit.
    */
   ROUNDABOUT_EXIT_4(30, "roundabout_e4"),
   /**
    * Enter the roundabout and take the 5th exit.
    */
   ROUNDABOUT_EXIT_5(31, "roundabout_e5"),
   /**
    * Enter the roundabout and take the 6th exit.
    */
   ROUNDABOUT_EXIT_6(32, "roundabout_e6"),
   /**
    * Enter the roundabout and take the 7th exit.
    */
   ROUNDABOUT_EXIT_7(33, "roundabout_e7"),
   /**
    * Enter the roundabout and take the 8th exit.
    */
   ROUNDABOUT_EXIT_8(34, "roundabout_e8"),
   /**
    * Pass POI.
    */
   PASS_PLACE(50, "pass_place");
*/

// ROUNDABOUT_EXIT - co s nima, u nas vpravo, v UK vlevo, ignoruju...

val routePointActionsToCoursePoints: Map<PointRteAction, CoursePoint> = mapOf(
        PointRteAction.LEFT_SLIGHT to CoursePoint.SLIGHT_LEFT,
        PointRteAction.STAY_LEFT to CoursePoint.LEFT_FORK,
        PointRteAction.RAMP_ON_LEFT to CoursePoint.LEFT_FORK,
        PointRteAction.MERGE_LEFT to CoursePoint.SLIGHT_LEFT,
        PointRteAction.LEFT to CoursePoint.LEFT,
        PointRteAction.LEFT_SHARP to CoursePoint.SHARP_LEFT,
        PointRteAction.U_TURN_LEFT to CoursePoint.SHARP_LEFT,
        PointRteAction.EXIT_LEFT to CoursePoint.LEFT,
        PointRteAction.RIGHT_SLIGHT to CoursePoint.SLIGHT_RIGHT,
        PointRteAction.STAY_RIGHT to CoursePoint.RIGHT_FORK,
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

val styleNameORIconStyleIconUrlToCoursePoints: Map<String, CoursePoint> = mapOf(
        "Summit.png" to CoursePoint.SUMMIT,
        "Dropoff.png" to CoursePoint.VALLEY,

        "tourism-drinkingwater.png" to CoursePoint.WATER,
        "Drinking Water.png" to CoursePoint.WATER,

        "restaurant-restaurant.png" to CoursePoint.FOOD,
        "Restaurant.png" to CoursePoint.FOOD,
        "restaurant-fastfood.png" to CoursePoint.FOOD,
        "Pizza.png" to CoursePoint.FOOD,
        "restaurant-pizza.png" to CoursePoint.FOOD,

        "sport-firstaid.png" to CoursePoint.FIRST_AID,
        "sport-hospital.png" to CoursePoint.FIRST_AID,
        "sport-doctor.png" to CoursePoint.FIRST_AID,
        "health-emergencyphone.png" to CoursePoint.FIRST_AID,
        "Medical Facility.png" to CoursePoint.FIRST_AID,

        "transport-accident.png" to CoursePoint.DANGER,
        "Skull and Crossbones.png" to CoursePoint.DANGER
)

val coursePointsDisplayOrder: List<CoursePoint> = listOf(
        CoursePoint.DANGER,

        CoursePoint.FIRST_AID,

        CoursePoint.GENERIC,

        CoursePoint.WATER,
        CoursePoint.FOOD,

        CoursePoint.SUMMIT,
        CoursePoint.VALLEY,

        CoursePoint.SHARP_LEFT,
        CoursePoint.LEFT,
        CoursePoint.SLIGHT_LEFT,
        CoursePoint.LEFT_FORK,

        CoursePoint.SHARP_RIGHT,
        CoursePoint.RIGHT,
        CoursePoint.SLIGHT_RIGHT,
        CoursePoint.RIGHT_FORK,

        CoursePoint.U_TURN,

        CoursePoint.STRAIGHT,
        CoursePoint.MIDDLE_FORK,

        CoursePoint.FOURTH_CATEGORY,
        CoursePoint.THIRD_CATEGORY,
        CoursePoint.SECOND_CATEGORY,
        CoursePoint.FIRST_CATEGORY,
        CoursePoint.HORS_CATEGORY,
        CoursePoint.SPRINT,

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
        5 to listOf(PointRteAction.STAY_LEFT, PointRteAction.STAY_RIGHT),
        6 to listOf(PointRteAction.RAMP_ON_LEFT, PointRteAction.RAMP_ON_RIGHT),
        7 to listOf(PointRteAction.MERGE_LEFT, PointRteAction.MERGE_RIGHT),
        8 to listOf(PointRteAction.LEFT_SLIGHT, PointRteAction.RIGHT_SLIGHT),
        9 to listOf(PointRteAction.EXIT_LEFT, PointRteAction.EXIT_RIGHT),
        10 to listOf(PointRteAction.U_TURN_LEFT, PointRteAction.U_TURN_RIGHT),
        11 to listOf(PointRteAction.U_TURN),
        12 to listOf(PointRteAction.RIGHT_SHARP, PointRteAction.LEFT_SHARP),
        13 to listOf(PointRteAction.RIGHT, PointRteAction.LEFT),
        14 to listOf(PointRteAction.PASS_PLACE)
)

class AttachWaypointsToTrack(val trackContainer: TrackContainer) {

    // try to assign (trackpoints) indices to waypoints with PointRteAction.UNDEFINED
    // that lay close enough to track

    var debugMessages = mutableListOf<String>()
        private set
    private var tag = "AttachWaypoints"

    // https://drive.google.com/open?id=1PEPjcHli7wXzy4iCc9TiGRuv7SIGzL8j
    fun rebuild(debug: Boolean): List<WaypointSimplified> {

        val waypoints = trackContainer.track.waypoints.toMutableList() // IS POINT
        val indicesTaken = mutableSetOf<Int>()
        val waypointsUndefined = mutableListOf<Point>() // IS POINT
        val waypointsSimplified = mutableListOf<WaypointSimplified>()

        if (debug) debugMessages.add("Size of all waypoints received in rebuild() ${waypoints.size}")

        // move all PointRteAction.UNDEFINED waypoints to waypointsUndefined
        waypointsUndefined.addAll(waypoints.filter { it.parameterRteAction == PointRteAction.UNDEFINED })
        waypoints.removeAll(waypointsUndefined)

        if (debug) {
            debugMessages.add("locations of waypoints got from shiftedRteIndex +++++++++++++++++++")
            trackContainer.definedRteActionsToShiftedIndices.forEach {
                val index = it.value
                val loc = trackContainer.track.points[index]
                debugMessages.add(" location -- ${LocationStringDump.locationStringDescriptionSimple(loc)}")
            }
            debugMessages.add("size: ${trackContainer.definedRteActionsToShiftedIndices.size}")
            debugMessages.add("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")
        }

        // copy waypoints to waypointsSimplified
        waypoints.forEach {
            val shiftedRteIndex: Int? = trackContainer.definedRteActionsToShiftedIndices[it]
            if (shiftedRteIndex != null) {
                waypointsSimplified.add(WaypointSimplified(
                        shiftedRteIndex,
                        it.name,
                        it.parameterRteAction,
                        it.getCoursepointEnumForced()
                ))
            } else if (debug) {
                val message = "shiftedRteIndex = null!"
                debugMessages.add(message)
                Log.e(tag, message)
                throw RuntimeException(message)
            }
        }

        if (debug){
            debugMessages.add("Size of waypoints after UNDEFINED removed ${waypoints.size}")
            debugMessages.add("Size of waypointsUndefined removed from waypoints ${waypointsUndefined.size}")
        }

        // mark trackpoints indices of waypoints as taken
        waypointsSimplified.forEach {
            if (it.rteIndex == -1) {
                val message = "rebuild - unexpected -1 from it.rteIndex"
                Log.w(tag, message)
                if (debug) debugMessages.add(message)
            }
            indicesTaken.add(it.rteIndex)
        }

        // now try to attach waypointsUndefined to not yet taken route indices
        waypointsUndefined.forEach {
            val waypoint = it
            val listIndicesCloseEnough = getSortedListOfIndicesCloseEnough(waypoint)
            for (i in listIndicesCloseEnough.indices) {
                val consideredRteIndex = listIndicesCloseEnough[i]
                if (!indicesTaken.contains(consideredRteIndex)) {
                    if (consideredRteIndex in trackContainer.track.points.indices &&
                            trackContainer.track.points[consideredRteIndex] != null) {
                        // mark index as taken
                        indicesTaken.add(consideredRteIndex)
                        // build new simplified waypoint
                        val attachedWaypoint = WaypointSimplified(
                                consideredRteIndex,
                                waypoint.getWaypointName(),
                                PointRteAction.PASS_PLACE,
                                waypoint.getCoursepointEnumForced()
                        )
                        // add it to attachedWaypoints
                        waypointsSimplified.add(attachedWaypoint)
                        if (debug) debugMessages.add("Attached new simplified waypoint: $attachedWaypoint")
                    } else if (debug) {
                        val debugMessage = "ERROR: index NOT IN trackContainer.track.points.indices OR null element"
                        debugMessages.add(debugMessage)
                        Log.e(tag, debugMessage)
                    }
                    break
                }
            }
        }
        waypointsSimplified.sort()
        if (debug) {
            debugMessages.add("Sorted rebuilt simplified waypoints")
            waypointsSimplified.forEach { debugMessages.add(it.toString()) }
        }
        return waypointsSimplified // rebuild
    }

    private fun getSortedListOfIndicesCloseEnough(waypoint: Point): List<Int> {
        val list = mutableListOf<Int>()
        val listDistInd = mutableListOf<IndexDistance>()
        var dist: Float
        for (i in trackContainer.track.points.indices) {
            if (trackContainer.track.points[i] == null) continue
            dist = waypoint.location.distanceTo(trackContainer.track.points[i])
            if (dist < MAX_DISTANCE_TO_CLIP_WP_TO_COURSE)
                listDistInd.add(IndexDistance(i, dist))
        }
        with(listDistInd) { sort(); forEach { list.add(it.index) } }
        return list
    }
}

data class IndexDistance(val index: Int, val distance: Float) : Comparable<IndexDistance> {
    override fun compareTo(other: IndexDistance): Int = this.distance.compareTo(other.distance)
}
