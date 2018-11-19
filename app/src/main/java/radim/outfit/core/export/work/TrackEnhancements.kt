package radim.outfit.core.export.work

import locus.api.objects.extra.Track
import locus.api.objects.utils.LocationCompute

// track does not contain null elements
fun extractPointTimestampsFromPoints(track: Track): List<Long>{
    val timestampMutableList = mutableListOf<Long>()
    track.points.forEach{
        timestampMutableList.add(it.time)
    }
    return timestampMutableList
}

fun assignPointTimestampsToNonNullPoints(track: Track, distances: List<Float>): List<Long>{
    val mutableListOfTimestamps = mutableListOf<Long>()
    var count = 0
    for( i in 0 until track.points.size) {
        if (track.points[i] == null) continue
        val dst = distances[count]
        if(dst == 0F){
            mutableListOfTimestamps.add(0)
            count ++
            continue
        }
        val timeMilis = (dst / DEF_SPEED_M_PER_S) * 1000F
        mutableListOfTimestamps.add(timeMilis.toLong())
        count ++
    }
    return mutableListOfTimestamps
}

fun assignPointDistancesToNonNullPoints(track: Track): List<Float>{
    val mutableListOfDistances = mutableListOf<Float>()
    var firstZeroSet = false
    var lastPoint = track.points[0]
    var sum = 0F
    for( i in 0 until track.points.size){
        if(track.points[i] == null) continue
        if(!firstZeroSet) {
            mutableListOfDistances.add(0F)
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

data class TrackTimestampsBundle(val startTime: Long, val totalTime: Float, val pointStamps: List<Long>)