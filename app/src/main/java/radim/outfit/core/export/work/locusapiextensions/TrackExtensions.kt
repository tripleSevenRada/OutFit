package radim.outfit.core.export.work.locusapiextensions

import locus.api.objects.extra.Track
import locus.api.objects.extra.TrackStats

fun TrackStats.isTimestamped(): Boolean{
    return ((this.startTime != this.stopTime) &&
            this.startTime > 100L &&
            this.stopTime > 100L &&
            this.stopTime > this.startTime &&
            this.totalTime > 100L
            )
}

fun Track.isTimestamped(): Boolean{
    var lastTimestamp = 100L
    this.points.forEach{
        if(it != null) {
            if (it.time < lastTimestamp) return false
            lastTimestamp = it.time
        }
    }
    return true
}

fun Track.hasAltitude(): Boolean{
    return !this.points.any{(it != null) && (!it.hasAltitude())}
}

// TODO lap property
fun Track.hasAltitudeTotals(): Boolean{
    return false
}
// TODO lap property
fun Track.hasAltitudeBounds(): Boolean{
    return false
}
