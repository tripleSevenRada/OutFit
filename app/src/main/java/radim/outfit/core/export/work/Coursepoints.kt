package radim.outfit.core.export.work

import android.util.Log
import com.garmin.fit.CoursePoint
import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Point
import locus.api.objects.utils.LocationCompute.computeDistanceFast
import radim.outfit.core.export.work.locusapiextensions.track_preprocessing.TrackContainer
import radim.outfit.core.export.work.locusapiextensions.WaypointSimplified
import radim.outfit.core.export.work.locusapiextensions.getCoursepointEnumForced
import radim.outfit.core.export.work.locusapiextensions.getWaypointName
import radim.outfit.core.export.work.locusapiextensions.stringdumps.LocationStringDump
import radim.outfit.core.export.work.locusapiextensions.track_preprocessing.MIN_DIST_MOVE_CONSIDER
import radim.outfit.core.export.work.locusapiextensions.track_preprocessing.MoveFunctions
import kotlin.system.exitProcess

const val COURSEPOINTS_LIMIT = 100
const val COURSEPOINTS_NAME_MAX_LENGTH = 20
const val MAX_DISTANCE_TO_CLIP_WP_TO_COURSE = 320.0F

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

val routePointActionsToCoursePoints: Map<PointRteAction, CoursePoint> = mapOf(
        PointRteAction.LEFT_SLIGHT to CoursePoint.SLIGHT_LEFT,
        PointRteAction.STAY_LEFT to CoursePoint.SLIGHT_LEFT,
        PointRteAction.RAMP_ON_LEFT to CoursePoint.LEFT_FORK,
        PointRteAction.MERGE_LEFT to CoursePoint.SLIGHT_LEFT,
        PointRteAction.LEFT to CoursePoint.LEFT,
        PointRteAction.LEFT_SHARP to CoursePoint.SHARP_LEFT,
        PointRteAction.U_TURN_LEFT to CoursePoint.SHARP_LEFT,
        PointRteAction.EXIT_LEFT to CoursePoint.LEFT,
        PointRteAction.RIGHT_SLIGHT to CoursePoint.SLIGHT_RIGHT,
        PointRteAction.STAY_RIGHT to CoursePoint.SLIGHT_RIGHT,
        PointRteAction.RAMP_ON_RIGHT to CoursePoint.RIGHT_FORK,
        PointRteAction.MERGE_RIGHT to CoursePoint.SLIGHT_RIGHT,
        PointRteAction.RIGHT to CoursePoint.RIGHT,
        PointRteAction.RIGHT_SHARP to CoursePoint.SHARP_RIGHT,
        PointRteAction.U_TURN_RIGHT to CoursePoint.SHARP_RIGHT,
        PointRteAction.EXIT_RIGHT to CoursePoint.RIGHT,
        PointRteAction.ROUNDABOUT_EXIT_1 to CoursePoint.GENERIC,
        PointRteAction.ROUNDABOUT_EXIT_2 to CoursePoint.GENERIC,
        PointRteAction.ROUNDABOUT_EXIT_3 to CoursePoint.GENERIC,
        PointRteAction.ROUNDABOUT_EXIT_4 to CoursePoint.GENERIC,
        PointRteAction.ROUNDABOUT_EXIT_5 to CoursePoint.GENERIC,
        PointRteAction.ROUNDABOUT_EXIT_6 to CoursePoint.GENERIC,
        PointRteAction.ROUNDABOUT_EXIT_7 to CoursePoint.GENERIC,
        PointRteAction.ROUNDABOUT_EXIT_8 to CoursePoint.GENERIC,
        PointRteAction.NO_MANEUVER to CoursePoint.GENERIC,
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
        "Summit" to CoursePoint.SUMMIT,
        "Dropoff" to CoursePoint.VALLEY,

        "tourism-drinkingwater.png" to CoursePoint.WATER,
        "restaurant-teahouse.png" to CoursePoint.WATER,
        "restaurant-coffee.png" to CoursePoint.WATER,
        "restaurant-bar.png" to CoursePoint.WATER,
        "restaurant-winery.png" to CoursePoint.WATER,
        "Drinking Water.png" to CoursePoint.WATER,
        "Bar.png" to CoursePoint.WATER,
        "Winery.png" to CoursePoint.WATER,
        "Fast Food.png" to CoursePoint.WATER,

        "Restaurant.png" to CoursePoint.FOOD,
        "Pizza.png" to CoursePoint.FOOD,
        "restaurant-bandb.png" to CoursePoint.FOOD,
        "restaurant-restaurant.png" to CoursePoint.FOOD,
        "restaurant-fastfood.png" to CoursePoint.FOOD,
        "restaurant-icecream.png" to CoursePoint.FOOD,
        "restaurant-pizza.png" to CoursePoint.FOOD,
        "stores-convenience.png" to CoursePoint.FOOD,

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
        5 to listOf(PointRteAction.ROUNDABOUT_EXIT_1,
                PointRteAction.ROUNDABOUT_EXIT_2,
                PointRteAction.ROUNDABOUT_EXIT_3,
                PointRteAction.ROUNDABOUT_EXIT_4,
                PointRteAction.ROUNDABOUT_EXIT_5,
                PointRteAction.ROUNDABOUT_EXIT_6,
                PointRteAction.ROUNDABOUT_EXIT_7,
                PointRteAction.ROUNDABOUT_EXIT_8),
        6 to listOf(PointRteAction.STAY_LEFT, PointRteAction.STAY_RIGHT),
        7 to listOf(PointRteAction.RAMP_ON_LEFT, PointRteAction.RAMP_ON_RIGHT),
        8 to listOf(PointRteAction.MERGE_LEFT, PointRteAction.MERGE_RIGHT),
        9 to listOf(PointRteAction.LEFT_SLIGHT, PointRteAction.RIGHT_SLIGHT),
        10 to listOf(PointRteAction.EXIT_LEFT, PointRteAction.EXIT_RIGHT),
        11 to listOf(PointRteAction.U_TURN_LEFT, PointRteAction.U_TURN_RIGHT),
        12 to listOf(PointRteAction.U_TURN),
        13 to listOf(PointRteAction.RIGHT_SHARP, PointRteAction.LEFT_SHARP,
                PointRteAction.NO_MANEUVER),
        14 to listOf(PointRteAction.RIGHT, PointRteAction.LEFT),
        15 to listOf(PointRteAction.PASS_PLACE, PointRteAction.UNDEFINED)
)

class AttachWaypointsToTrack(val trackContainer: TrackContainer) {

    // try to assign (trackpoints) indices to waypoints with PointRteAction.UNDEFINED
    // that lay close enough to track

    var debugMessages = mutableListOf<String>()
        private set
    private var tag = "AttachWaypoints"

    // https://drive.google.com/open?id=1PEPjcHli7wXzy4iCc9TiGRuv7SIGzL8j
    fun rebuild(debug: Boolean, moveCustom: Boolean, moveDist: Double): List<WaypointSimplified> {

        val waypoints = trackContainer.track.waypoints.toMutableList() // IS POINT
        val indicesTaken = mutableSetOf<Int>()
        val waypointsUndefined = mutableListOf<Point>() // IS POINT
        val waypointsSimplified = mutableListOf<WaypointSimplified>()

        if (debug) debugMessages.add("Size of all waypoints received in rebuild() ${waypoints.size}")

        // move all PointRteAction.UNDEFINED waypoints to waypointsUndefined
        waypointsUndefined.addAll(waypoints.filter { it != null && it.parameterRteAction == PointRteAction.UNDEFINED })
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
                exitProcess(-1)
            }
        }

        if (debug) {
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

        if (debug) {
            debugMessages.add("Sorted rebuilt simplified waypoints")
            waypointsSimplified.forEach { debugMessages.add(it.toString()); Log.i(tag, "Wpt Simplified: $it") }
        }

        // move PASS_PLACE waypoints towards start if selected in export options
        if (moveCustom) {
            // new list of copies
            val waypointsSimplifiedCopies = mutableListOf<WaypointSimplified>()
            val functions = MoveFunctions()
            val indicesMoveTaken = mutableSetOf<Int>()

            fun startOrFirstNonTaken(indexStart: Int): Int {
                if (indexStart == 0 && indicesMoveTaken.contains(0)) return -1
                if (!indicesMoveTaken.contains(indexStart)) return indexStart
                var index = indexStart - 1
                val startLoc = trackContainer.track.getPoint(indexStart)
                while (index >= 0) {
                    val distFromStartIndex = computeDistanceFast(startLoc, trackContainer.track.getPoint(index))
                    if (distFromStartIndex > 100.0) return -1
                    if (!indicesMoveTaken.contains(index)) return index
                    else index--
                }
                return -1
            }

            // split waypointsSimplified into
            // -waypointsSimplified
            // -waypointsSimplifiedPassPlace
            val waypointsSimplifiedPassPlace =
                    waypointsSimplified.filter { it.rteAction == PointRteAction.PASS_PLACE }
            waypointsSimplified.removeAll(waypointsSimplifiedPassPlace)

            waypointsSimplified.forEach {
                waypointsSimplifiedCopies.add(WaypointSimplified(it, it.rteIndex))
                indicesMoveTaken.add(it.rteIndex)
            }

            if (debug) {
                debugMessages.add("indicesMoveTaken.size = ${indicesMoveTaken.size}")
                indicesMoveTaken.forEach { debugMessages.add("$it") }
                for (i in trackContainer.track.points.indices) {
                    val moved = startOrFirstNonTaken(i)
                    if (moved != i) debugMessages.add("i $i, moved $moved")
                }
            }

            // move waypointsSimplifiedPassPlace
            waypointsSimplifiedPassPlace.forEach {
                val leftRightCoef = functions.getPrePostAndCoef(
                        it.rteIndex,
                        moveDist,
                        trackContainer.track,
                        MIN_DIST_MOVE_CONSIDER
                )
                val indexCloserToDesiredLocation = if (leftRightCoef.third > 0.5) leftRightCoef.first
                else leftRightCoef.second

                //Log.e(tag, "orig index: ${it.rteIndex}")
                //Log.e(tag, "desired index: $indexCloserToDesiredLocation")

                val startOrFirstNonTaken = startOrFirstNonTaken(indexCloserToDesiredLocation)
                if (startOrFirstNonTaken != -1) {
                    // do move
                    indicesMoveTaken.add(startOrFirstNonTaken)
                    waypointsSimplifiedCopies.add(WaypointSimplified(it, startOrFirstNonTaken))
                } else {
                    // no move at all
                    indicesMoveTaken.add(it.rteIndex)
                    waypointsSimplifiedCopies.add(WaypointSimplified(it, it.rteIndex))
                }
            }

            if (debug) {
                logWptLines(waypointsSimplifiedCopies)
            }

            waypointsSimplifiedCopies.sort()
            return waypointsSimplifiedCopies
        }

        if (debug) {
            logWptLines(waypointsSimplified)
        }

        waypointsSimplified.sort()
        return waypointsSimplified// rebuild
    }

    private fun getSortedListOfIndicesCloseEnough(waypoint: Point): List<Int> {
        val list = mutableListOf<Int>()
        val listDistInd = mutableListOf<IndexDistance>()
        var dist: Double
        for (i in trackContainer.track.points.indices) {
            if (trackContainer.track.points[i] == null) continue
            dist = computeDistanceFast(waypoint.location, trackContainer.track.points[i])
            if (dist < MAX_DISTANCE_TO_CLIP_WP_TO_COURSE)
                listDistInd.add(IndexDistance(i, dist))
        }
        with(listDistInd) { sort(); forEach { list.add(it.index) } }
        return list
    }

    private fun logWptLines(data: Iterable<WaypointSimplified>) {
        data.forEach {
            if (it.rteAction == PointRteAction.PASS_PLACE) {
                System.out.println("<wpt lat=\"${trackContainer.track.getPoint(it.rteIndex).latitude}\"" +
                        " lon=\"${trackContainer.track.getPoint(it.rteIndex).longitude}\"></wpt>")
            }
        }
    }
}

data class IndexDistance(val index: Int, val distance: Double) : Comparable<IndexDistance> {
    override fun compareTo(other: IndexDistance): Int = this.distance.compareTo(other.distance)
}
