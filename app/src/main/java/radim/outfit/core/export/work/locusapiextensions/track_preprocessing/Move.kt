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

const val MIN_DIST_MOVE_CONSIDER = 1.0

class Move(val debugMessages: MutableList<String>) {

    fun move(moveDist: Double, trackContainer: TrackContainer): TrackContainer {

        val tag = "MOVE"

        // trackContainer.definedRteActionsToShiftedIndices contains all except UNDEFINED

        // memoization START
        val wpToOriginalIndex = mutableMapOf<Point, Int>()
        val wpToOriginalLoc = mutableMapOf<Point, Location>()
        val passPlaceStash = mutableListOf<Point>()
        val undefinedStash = mutableListOf<Point>()

        trackContainer.track.waypoints.forEach {
            if (it.parameterRteAction != PointRteAction.UNDEFINED) {
                if (DEBUG_MODE && it.paramRteIndex == -1) {
                    Log.e(tag, "it.paramRteIndex == -1 AND NOT UNDEFINED")
                    exitProcess(-1)
                }
                wpToOriginalLoc[it] = trackContainer.track.points[trackContainer.definedRteActionsToShiftedIndices[it]
                        ?: 0]
                wpToOriginalIndex[it] = trackContainer.definedRteActionsToShiftedIndices[it] ?: 0
                if (it.parameterRteAction == PointRteAction.PASS_PLACE) passPlaceStash.add(it)
            } else undefinedStash.add(it)
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
                val functions = MoveFunctions()
                val prePostCoefTriple = functions.getPrePostAndCoef(index, moveDist, trackContainer.track, MIN_DIST_MOVE_CONSIDER)
                // if prePostPair equals -1, -1 nothing is going to happen
                if (with(prePostCoefTriple) { first != -1 && second != -1 }) {
                    // we do have a triple, we can insert new trackpoint
                    val locToInsert = functions.locationFactory(trackContainer, prePostCoefTriple)
                    if (DEBUG_MODE) movedLocWPTdebug.add(locToInsert.toWptRecord())
                    // actually insert
                    trackContainer.track.points.add(prePostCoefTriple.second, locToInsert)
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
            if (wpToMovedLoc.size != rteActionsOnlyWP.size) exitProcess(-4)
            debugMessages.add("MOVE: ORIGINALS"); debugMessages.addAll(origLocWPTdebug)
            debugMessages.add("MOVE: MOVED"); debugMessages.addAll(movedLocWPTdebug)
        }

        // TIDY UP

        // I need to rebuild trackContainer.definedRteActionsToShiftedIndices
        // and possibly the order of waypoints.(Swaps are possible).
        // I already have:
        //
        //     wpToOriginalIndex = mutableMapOf<Point, Int>()
        //     wpToOriginalLoc = mutableMapOf<Point, Location>()
        //     for everything but UNDEFINED
        //
        //
        //     wpToMovedLoc
        //     for rteActionsOnly
        //
        //     passPlaceStash = mutableListOf<Point>()
        //     undefinedStash = mutableListOf<Point>()

        val rebuiltWpoints: MutableList<WPIndex> = mutableListOf()
        fun addToRebuiltWpoints(point: Point, loc: Location?, indexStart: Int?) {
            if (DEBUG_MODE && (indexStart == null || loc == null)) {
                exitProcess(-5)
            }
            // search in both directions until you find index of moved location OR -1
            val indexMoved = if (loc != null && indexStart != null) MoveFunctions().locSearch(loc, indexStart, trackContainer)
            else -1
            // -1 means ERROR
            if (DEBUG_MODE && indexMoved == -1) {
                exitProcess(-6)
            }
            // put waypoint and moved index into WPIndex
            rebuiltWpoints.add(WPIndex(point, indexMoved))
        }

        // iterate rteActionsOnlyWP
        rteActionsOnlyWP.forEach { addToRebuiltWpoints(it, wpToMovedLoc[it], wpToOriginalIndex[it]) }
        // iterate stashed PASS_PLACE
        passPlaceStash.forEach { addToRebuiltWpoints(it, wpToOriginalLoc[it], wpToOriginalIndex[it]) }
        // sort list of <WPIndex> by indices
        rebuiltWpoints.sort()

        // clear & copy into trackContainer.track.waypoints
        // put into newDefinedRteActionsToShiftedIndices
        trackContainer.track.waypoints.clear()
        val newDefinedRteActionsToShiftedIndices = mutableMapOf<Point, Int>()

        rebuiltWpoints.forEach {
            if (it.index != -1) {
                trackContainer.track.waypoints.add(it.wp)
                newDefinedRteActionsToShiftedIndices[it.wp] = it.index
            }
        }
        undefinedStash.forEach { trackContainer.track.waypoints.add(it) }

        val clusterOldContainer = trackContainer.clusterize
        val moveOldContainer = trackContainer.move
        val moveDistOldContainer = trackContainer.moveDist

        return TrackContainer(trackContainer.track,
                newDefinedRteActionsToShiftedIndices,
                "",
                clusterOldContainer,
                moveOldContainer,
                moveDistOldContainer)
    }

    private data class WPIndex(val wp: Point, val index: Int) : Comparable<WPIndex> {
        override fun compareTo(other: WPIndex): Int {
            return this.index.compareTo(other.index)
        }
    }
}

class MoveFunctions {
    // returns -1 -1 if no pair found
    fun getPrePostAndCoef(index: Int, moveDist: Double, track: Track, minDistConsider: Double): Triple<Int, Int, Double> {
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

    // search in track for loc, start @index
    // if loc not found return -1
    fun locSearch(loc: Location, start: Int, trackContainer: TrackContainer): Int {
        val trackpoints = trackContainer.track.points
        if (start !in trackpoints.indices) return -1
        if (trackpoints[start] === loc) return start
        var locSearchCount = 1
        while (true) {
            val left = start - locSearchCount
            val right = start + locSearchCount
            if (left in trackpoints.indices && trackpoints[left] === loc) return left
            if (right in trackpoints.indices && trackpoints[right] === loc) return right
            if (left !in trackpoints.indices && right !in trackpoints.indices) return -1
            locSearchCount++
        }
    }

    fun locationFactory(trackContainer: TrackContainer, prePostCoefTriple: Triple<Int,Int,Double>): Location{
        val right = trackContainer.track.points[prePostCoefTriple.second]
        val left = trackContainer.track.points[prePostCoefTriple.first]
        val lat = linearInterpolatorGeneric(right.latitude, left.latitude, prePostCoefTriple.third)
        val lon = linearInterpolatorGeneric(right.longitude, left.longitude, prePostCoefTriple.third)
        val locationProduced = Location()
        with(locationProduced) { latitude = lat; longitude = lon }
        if (left.hasAltitude() && right.hasAltitude()) {
            locationProduced.altitude =
                    linearInterpolatorGeneric(right.altitude, left.altitude, prePostCoefTriple.third)
        }
        if (left.time > 100L && right.time > 100L) {
            locationProduced.time =
                    linearInterpolatorGeneric(right.time, left.time, prePostCoefTriple.third)
        }
        return locationProduced
    }

}