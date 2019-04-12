package radim.outfit.core.export.work.locusapiextensions

import android.util.Log
import locus.api.objects.extra.Track
import locus.api.objects.utils.LocationCompute
import radim.outfit.DEBUG_MODE
import radim.outfit.core.export.work.routePointActionsPrioritized
import radim.outfit.core.export.work.routePointActionsToCoursePoints
import kotlin.system.exitProcess

// track does not contain null elements and is fully timestamped
fun extractPointTimestampsFromPoints(track: Track): List<Long> {
    val mutableListOfTimestamps = mutableListOf<Long>()
    track.points.forEach {
        mutableListOfTimestamps.add(it.time)
    }
    return mutableListOfTimestamps
}

// track may contain null elements and is not considered fully timestamped
fun assignPointTimestampsToNonNullPoints(track: Track, distances: List<Float>, speedIfNotInTrack: Float): List<Long> {
    val now: Long = System.currentTimeMillis()
    val mutableListOfTimestamps = mutableListOf<Long>()
    var count = 0
    for (i in track.points.indices) {
        if (track.points[i] == null) continue
        // first point
        if (count == 0) {
            mutableListOfTimestamps.add(now)
            count = 1
            continue
        }
        val dst = distances[count]
        val timeMilis = (dst / speedIfNotInTrack) * 1000F
        mutableListOfTimestamps.add(now + timeMilis.toLong())
        count++
    }
    return mutableListOfTimestamps
}

fun assignPointDistancesToNonNullPoints(track: Track): List<Float> {
    val mutableListOfDistances = mutableListOf<Float>()
    var firstZeroSet = false
    var lastPoint = track.points[0]
    var sum = 0F
    for (i in track.points.indices) {
        if (track.points[i] == null) continue
        if (!firstZeroSet) {
            mutableListOfDistances.add(sum) // 0F
            lastPoint = track.points[i]
            firstZeroSet = true
            continue
        }
        val dst = LocationCompute.computeDistanceFast(
                lastPoint.latitude, lastPoint.longitude,
                track.points[i].latitude, track.points[i].longitude
        ).toFloat()
        sum += dst
        mutableListOfDistances.add(sum)
        lastPoint = track.points[i]
    }
    return mutableListOfDistances
}

fun assignSpeedsToNonNullPoints(track: Track, timeBundle: TrackTimestampsBundle, dist: List<Float>): List<Float> {
    val speeds = mutableListOf<Float>()
    var index = 0
    var lastTime: Long = timeBundle.pointStamps[0]
    var lastDist: Float = dist[0]
    for (i in track.points.indices) {
        if (track.points[i] == null) continue
        var timeDeltaInS: Float = ((timeBundle.pointStamps[index] - lastTime).toFloat()) / 1000F
        var distDeltaInM: Float = dist[index] - lastDist
        if (timeDeltaInS < 0F) timeDeltaInS = 0F
        if (distDeltaInM < 0F) distDeltaInM = 0F
        val speed = distDeltaInM / timeDeltaInS
        speeds.add(if (speed.isFinite()) speed else 0F)
        lastTime = timeBundle.pointStamps[index]
        lastDist = dist[index]
        index++
    }
    if (DEBUG_MODE && (timeBundle.pointStamps.size != dist.size || dist.size != speeds.size)) {
        Log.e("TRACK_ENHANCEMENTS", "Data sizes - TrackEnhancements")
        exitProcess(-1)
    }
    return speeds
}


// According do docs it should be possible to position a coursepoint just using a related trackpoint's timestamp ,
// but I better don't trust it as Garmin itself does not use that simple approach


fun mapNonNullPointsIndicesToTimestamps(track: Track, timeBundle: TrackTimestampsBundle): Map<Int, Long> {
    val nonNullTimestamps = timeBundle.pointStamps
    return mapNonNullIndicesToValues(track, nonNullTimestamps)
}

fun mapNonNullPointsIndicesToDistances(track: Track, distances: List<Float>): Map<Int, Float>{
    return mapNonNullIndicesToValues(track, distances)
}

fun <V> mapNonNullIndicesToValues(track: Track, values: List<V>): Map<Int,V>{
    val indicesToNonNullValues = mutableMapOf<Int, V>()
    var indexNonNull = 0
    for (index in track.points.indices) {
        if (track.points[index] == null) continue
        indicesToNonNullValues[index] = values[indexNonNull]
        if (indexNonNull == values.size)
            throw RuntimeException("Indices messed 1 - TrackEnhancements")
        indexNonNull++
    }
    if(indicesToNonNullValues.size != values.size)
        throw RuntimeException("Indices messed 2 - TrackEnhancements")
    return indicesToNonNullValues
}

fun ridUnsupportedRtePtActions(waypoints: List<WaypointSimplified>): List<WaypointSimplified>{
    return waypoints.filter { routePointActionsToCoursePoints.keys.contains(it.rteAction) }
}

fun reduceWayPointsSizeTo(points: List<WaypointSimplified>, limit: Int): List<WaypointSimplified>{
    var toReduce = points.toMutableList()
    val range = IntRange(1, routePointActionsPrioritized.size - 1) // allways keep all PASS_PLACE and UNDEFINED waypoints here
    for (i in range){
        if (toReduce.size <= limit) break
        val rteActionsToRid = routePointActionsPrioritized[i] ?: listOf()
        toReduce = toReduce.filter { ! rteActionsToRid.contains(it.rteAction) }.toMutableList()
    }
    // now reduce even PASS_PLACE and UNDEFINED if necessary
    return if (toReduce.size <= limit) toReduce
    else {
        val over = toReduce.size - limit
        val pre: Int
        val post: Int
        if(over % 2 == 0){pre = over / 2; post = over / 2}
        else{pre = (over / 2) + 1; post = over/2}
        toReduce.subList(pre, toReduce.size - post)
    }
}

data class TrackTimestampsBundle(val startTime: Long, val totalTime: Float, val pointStamps: List<Long>)


