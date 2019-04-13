package radim.outfit.core.export.work.locusapiextensions.track_preprocessing

import android.util.Log
import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Location
import locus.api.objects.extra.Point
import locus.api.objects.extra.Track
import locus.api.objects.utils.LocationCompute.computeDistanceFast
import radim.outfit.DEBUG_MODE
import radim.outfit.core.export.work.locusapiextensions.toWptRecord
import kotlin.system.exitProcess

class Move(val debugMessages: MutableList<String>) {

    private val minDistConsider = 1.0

    fun move(moveDist: Double, trackContainer: TrackContainer): TrackContainer {

        val tag = "MOVE"

        // trackContainer.definedRteActionsToShiftedIndices contains all except UNDEFINED

        // memoization START
        val wpToOriginalIndex = mutableMapOf<Point, Int>()
        val wpToOriginalLoc = mutableMapOf<Point, Location>()

        trackContainer.track.waypoints.forEach {
            if (it.parameterRteAction != PointRteAction.UNDEFINED) {
                if (DEBUG_MODE && it.paramRteIndex == -1) {
                    Log.e(tag, "it.paramRteIndex == -1 AND NOT UNDEFINED")
                    exitProcess(-1)
                }
                wpToOriginalLoc[it] = trackContainer.track.points[trackContainer.definedRteActionsToShiftedIndices[it]
                        ?: 0]
                wpToOriginalIndex[it] = trackContainer.definedRteActionsToShiftedIndices[it] ?: 0
            }
        }

        val trackLocToOrigIndex = mutableMapOf<Location, Int>()
        var count = 0

        trackContainer.track.points.forEach { trackLocToOrigIndex[it] = count; count++ }
        // memoization END

        val wpToMovedLoc = mutableMapOf<Point, Location>()

        // debug asserts
        if (DEBUG_MODE) {
            trackContainer.track.waypoints.forEach {
                if (it.parameterRteAction == PointRteAction.UNDEFINED && it.paramRteIndex != -1) {
                    Log.e(tag, "PointRteAction.UNDEFINED && it.paramRteIndex != -1")
                    exitProcess(-1)
                }
                if (it.parameterRteAction != PointRteAction.UNDEFINED &&
                        it.parameterRteAction != PointRteAction.PASS_PLACE) {
                    val index1 = trackContainer.definedRteActionsToShiftedIndices[it]
                    val index2 = getCurrentIndexOf(
                            trackContainer.track, trackContainer.track.points[index1
                            ?: 0], trackLocToOrigIndex)
                    if (index1 != index2) {
                        Log.e(tag, "index1 != index2")
                        exitProcess(-2)
                    }
                }
            }
        }

        // debug messages
        val origLocWPTdebug = mutableListOf<String>()
        val movedLocWPTdebug = mutableListOf<String>()

        // NOT PointRteAction.UNDEFINED; NOT PointRteAction.PASS_PLACE
        val rteActionsOnlyWP = RteActionsOnlyWP(trackContainer.track).getRteActionsOnlyWP()
        rteActionsOnlyWP.forEach { wptToBeShifted ->

            // beginning of shift procedure of wptToBeShifted
            val locThatBelongsToWptToBeShifted = wpToOriginalLoc[wptToBeShifted]
            if (DEBUG_MODE) origLocWPTdebug.add(locThatBelongsToWptToBeShifted?.toWptRecord()
                    ?: "UNEXPECTED NULL")
            if (locThatBelongsToWptToBeShifted != null) {
                val index = getCurrentIndexOf(
                        trackContainer.track,
                        locThatBelongsToWptToBeShifted,
                        trackLocToOrigIndex)
                val prePostCoefTriple = getPrePostAndCoef(index, moveDist, trackContainer.track)
                // if prePostPair equals -1, -1 nothing is going to happen
                if (with(prePostCoefTriple) { first != -1 && second != -1 }) {
                    // we do have a triple, we can insert new trackpoint
                    val right = trackContainer.track.points[prePostCoefTriple.second]
                    val left = trackContainer.track.points[prePostCoefTriple.first]
                    val lat = linearInterpolatorGeneric(right.latitude, left.latitude, prePostCoefTriple.third)
                    val lon = linearInterpolatorGeneric(right.longitude, left.longitude, prePostCoefTriple.third)
                    val locToInsert = Location()
                    with(locToInsert) { latitude = lat; longitude = lon }
                    if (left.hasAltitude() && right.hasAltitude()) {
                        locToInsert.altitude =
                                linearInterpolatorGeneric(right.altitude, left.altitude, prePostCoefTriple.third)
                    }
                    if (left.time > 100L && right.time > 100L) {
                        locToInsert.time =
                                linearInterpolatorGeneric(right.time, left.time, prePostCoefTriple.third)
                    }
                    if (DEBUG_MODE) movedLocWPTdebug.add(locToInsert.toWptRecord())
                    // actually insert
                    trackContainer.track.points[prePostCoefTriple.second] = locToInsert
                    // keep track
                    wpToMovedLoc[wptToBeShifted] = locToInsert
                } else {
                    // we do not have a triple, we cannot insert new trackpoint
                    // keep track
                    wpToMovedLoc[wptToBeShifted] = locThatBelongsToWptToBeShifted
                }
            } else if (DEBUG_MODE) exitProcess(-3)
        }

        if (DEBUG_MODE) {
            if(wpToMovedLoc.size != rteActionsOnlyWP.size) exitProcess(-4)
            debugMessages.add("MOVE: ORIGINALS"); debugMessages.addAll(origLocWPTdebug)
            debugMessages.add("MOVE: MOVED"); debugMessages.addAll(movedLocWPTdebug)
        }

        // tidy


        return trackContainer
    }

    // returns -1 -1 if no pair found
    private fun getPrePostAndCoef(index: Int, moveDist: Double, track: Track): Triple<Int, Int, Double> {
        var distAccumulator = 0.0
        var left = index - 1
        var right = index
        var coef = 0.5
        // coef 0.1 is close to right, 0.9 close to left
        if (index !in track.points.indices || index == 0) return Triple(-1, -1, coef)
        while (true) {
            val distRightLeft: Double = computeDistanceFast(track.points[right], track.points[left])
            if (moveDist >= distAccumulator &&
                    moveDist <= (distAccumulator + distRightLeft) &&
                    distRightLeft > minDistConsider) {
                val remnant = moveDist - distAccumulator
                coef = remnant / distRightLeft
                if (coef < 0.05) coef = 0.05
                else if (coef > 0.95) coef = 0.95
                return Triple(left, right, coef)
            }
            distAccumulator += distRightLeft
            right = left
            left = right - 1
            if (left !in track.points.indices) return Triple(-1, -1, coef)
        }
    }
}