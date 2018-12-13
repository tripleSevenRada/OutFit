package radim.outfit.core.export.work.locusapiextensions

import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Point
import locus.api.objects.extra.Track
import locus.api.objects.utils.LocationCompute
import radim.outfit.core.export.work.routePointActionsPrioritized
import radim.outfit.core.export.work.routePointActionsToCoursePoints

// track does not contain null elements and is fully timestamped
fun extractPointTimestampsFromPoints(track: Track): List<Long> {
    val timestampMutableList = mutableListOf<Long>()
    track.points.forEach {
        timestampMutableList.add(it.time)
    }
    return timestampMutableList
}

// track may contain null elements and is not considered fully timestamped
fun assignPointTimestampsToNonNullPoints(track: Track, distances: List<Float>, speedIfNotInTrack: Float): List<Long> {
    val now: Long = System.currentTimeMillis()
    val mutableListOfTimestamps = mutableListOf<Long>()
    var count = 0
    for (i in 0 until track.points.size) {
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
    for (i in 0 until track.points.size) {
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
    if (timeBundle.pointStamps.size != dist.size || dist.size != speeds.size)
        throw RuntimeException("Data sizes - TrackEnhancements")
    return speeds
}

fun mapNonNullPointsIndicesToTimestamps(track: Track, timeBundle: TrackTimestampsBundle): Map<Int, Long> {
    val nonNullTimestamps = timeBundle.pointStamps
    val indicesToNonNullTimestamps = mutableMapOf<Int, Long>()
    var indexNonNull = 0
    for (index in track.points.indices) {
        if (track.points[index] == null) continue
        indicesToNonNullTimestamps.put(index, nonNullTimestamps[indexNonNull])
        indexNonNull++
        if (indexNonNull == nonNullTimestamps.size)
            throw RuntimeException("Indices messed 1 - TrackEnhancements")
    }
    if(indicesToNonNullTimestamps.size != nonNullTimestamps.size)
        throw RuntimeException("Indices messed 2 - TrackEnhancements")
    return indicesToNonNullTimestamps
}

fun ridUnsupportedRtePtActions(waypoints: List<Point>): List<Point>{
    return waypoints.filter { routePointActionsToCoursePoints.keys.contains(it.parameterRteAction) }
}

fun reduceWayPointsSizeTo(points: List<Point>, limit: Int): List<Point>{
    var toReduce = points.toMutableList()
    val range = IntRange(1, routePointActionsPrioritized.size)
    for (i in range){
        val rteActionsToRid = routePointActionsPrioritized[i] ?: listOf()
        toReduce = toReduce.filter { ! rteActionsToRid.contains(it.parameterRteAction) }.toMutableList()
        if (toReduce.size < limit) break
    }
    return toReduce
}

data class TrackTimestampsBundle(val startTime: Long, val totalTime: Float, val pointStamps: List<Long>)