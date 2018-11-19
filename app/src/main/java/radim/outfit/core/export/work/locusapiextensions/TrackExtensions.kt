package radim.outfit.core.export.work.locusapiextensions

import locus.api.objects.extra.Track
import locus.api.objects.extra.TrackStats


fun TrackStats.isTimestamped():Boolean{
    return ((this.startTime != this.stopTime) &&
            this.startTime > 100L &&
            this.stopTime > 100L &&
            this.stopTime > this.startTime &&
            this.totalTime > 100L
            )
}

fun Track.isTimestamped(): Boolean{
    val points = this.points
    var lastTimestamp = 100L
    points.forEach{
        if(it != null) {
            if (it.time < lastTimestamp) return false
            lastTimestamp = it.time
        } else return false
    }
    return true
}
