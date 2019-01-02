package radim.outfit.debugasserts

import android.util.Log
import locus.api.objects.extra.Track
import radim.outfit.core.export.work.locusapiextensions.WaypointSimplified

const val LOG_TAG_C_ASSERTS = "CUSTOM_ASSERTS"

fun assertWaypointsAreLinkedToTrackpointsOneToOneIncreasing(waypoints: List<WaypointSimplified>): Boolean {
    val indicesTaken = mutableSetOf<Int>()
    var last = -1
    waypoints.forEach {
        if (it.rteIndex <= last) return false
        last = it.rteIndex
        if (indicesTaken.contains(it.rteIndex)) return false
        indicesTaken.add(it.rteIndex)
    }
    return indicesTaken.size == waypoints.size
}

fun assertTimestampsIncreasingOrEqualFullyTimestampedTrack(track: Track): Boolean {
    var last = 100L
    for (i in track.points.indices) {
        if (track.points[i] == null) continue

        if (track.points[i].time < last) return false
        if (track.points[i].time == last)
            Log.w(LOG_TAG_C_ASSERTS, "equal timestamps! - FullyTimestampedTrack")
        last = track.points[i].time
    }
    return true
}

fun <T : Comparable<T>> assertValuesIncreasingOrEqual(values: List<T>): Boolean {
    if (values.isNotEmpty()) {
        var last = values[0]
        var equal = 0
        for (i in values.indices) {
            if (values[i] < last) return false
            if (values[i] == last && i != 0) {
                // Log.w(LOG_TAG_F_UTILS, "i: $i of ${values.size} -- Equal values!
                // Type: ${last.javaClass}, last: $last now: ${values[i]}")
                equal++
            }
            last = values[i]
        }
        if (equal > 0) Log.w(LOG_TAG_C_ASSERTS, "Equal values: $equal out of ${values.size}")
    }
    return true
}

// fitSDK messages utility methods in Messages.kt do assert that reserved chars are replaced
// and lengths reduced
fun WaypointSimplified.hasProperName(): Boolean = name.isNotEmpty()

fun WaypointSimplified.hasValidRteIndex(track: Track): Boolean = rteIndex in track.points.indices &&
        track.points[rteIndex] != null
