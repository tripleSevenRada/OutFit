package radim.outfit.core.export.work.locusapiextensions.track_preprocessing

import android.util.Log
import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Location
import locus.api.objects.extra.Point
import locus.api.objects.extra.Track
import locus.api.objects.utils.LocationCompute.computeDistanceFast
import radim.outfit.DEBUG_MODE
import kotlin.system.exitProcess

class Move(val debugMessages: MutableList<String>) {

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
                wpToOriginalLoc[it] = trackContainer.track.points[trackContainer.definedRteActionsToShiftedIndices[it] ?: 0]
                wpToOriginalIndex[it] = trackContainer.definedRteActionsToShiftedIndices[it] ?: 0
            }
        }

        val trackLocToOrigIndex = mutableMapOf<Location, Int>()
        var count = 0

        trackContainer.track.points.forEach { trackLocToOrigIndex[it] = count; count++ }

        // memoization END

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

        // NOT PointRteAction.UNDEFINED; NOT PointRteAction.PASS_PLACE
        val rteActionsOnlyWP = RteActionsOnlyWP(trackContainer.track).getRteActionsOnlyWP()
        rteActionsOnlyWP.forEach { wptToBeShifted ->

            // beginning of shift procedure of wptToBeShifted
            val locThatBelongsToWptToBeShifted = wpToOriginalLoc[wptToBeShifted]
            if (locThatBelongsToWptToBeShifted != null) {
                val index = getCurrentIndexOf(
                        trackContainer.track,
                        locThatBelongsToWptToBeShifted,
                        trackLocToOrigIndex)
                val prePostPair = getPrePost(index, moveDist, trackContainer.track)
                // if prePostPair equals -1, -1 nothing is going to happen
                if (with (prePostPair) { first != -1 && second != -1}){
                    // we do have a pair to insert new trackpoint in
                    // TODO

                    val testLat = (trackContainer.track.points[prePostPair.first].latitude +
                            trackContainer.track.points[prePostPair.second].latitude) / 2
                    val testLon = (trackContainer.track.points[prePostPair.first].longitude +
                            trackContainer.track.points[prePostPair.second].longitude) / 2

                    val loc = Location()
                    loc.latitude = testLat
                    loc.longitude = testLon
                    val dist = computeDistanceFast(locThatBelongsToWptToBeShifted, loc)
                    Log.e(tag, "index: $index left: ${prePostPair.first} right: ${prePostPair.second} dist: $dist")



                }else {
                    // we do not have a pair to insert new trackpoint in
                    // TODO


                    Log.e(tag, "NO PAIR")


                }



            } else if (DEBUG_MODE) exitProcess(-3)


        }





















        return trackContainer
    }

    // returns -1 -1 if no pair found
    private fun getPrePost(index: Int, moveDist: Double, track: Track): Pair<Int, Int>{
        if(index !in track.points.indices || index == 0) return Pair(-1, -1)
        var distAccumulator = 0.0
        var left = index - 1
        var right = index
        while (true){
            val distRightLeft = computeDistanceFast(track.points[right], track.points[left])
            if(moveDist >= distAccumulator && moveDist <= distAccumulator + distRightLeft){
                return Pair(left, right)
            }
            distAccumulator += distRightLeft
            right = left
            left = right - 1
            if(left !in track.points.indices) return Pair(-1, -1)
        }
    }
}