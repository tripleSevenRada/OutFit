package radim.outfit.core.export.work.locusapiextensions

import locus.api.objects.extra.Location
import locus.api.objects.extra.Track
import locus.api.objects.extra.TrackStats
import radim.outfit.core.export.work.MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989

fun TrackStats.isTimestamped(): Boolean {
    return ((this.startTime != this.stopTime) &&
            this.startTime > 100L &&
            this.stopTime > 100L &&
            this.stopTime > this.startTime &&
            this.totalTime > 100L
            )
}

// track is considered fully timestamped iff does NOT contain null elements,
// is recent
// and is monotonic
fun Track.isTimestamped(): Boolean {
    var lastTimestamp = 100L
    this.points.forEach {
        if (it != null) {
            if (it.time < lastTimestamp) return false
            else if (it.time < MILIS_FROM_START_UNIX_ERA_TO_UTC_00_00_Dec_31_1989) return false
            lastTimestamp = it.time
        } else return false
    }
    return true
}

fun Track.getFirstNonNullPoint(): Location? {
    this.points.forEach { if(it != null) return it }
    return null
}

fun Track.getLastNonNullPoint(): Location? {
    this.points.asReversed().forEach { if(it != null) return it }
    return null
}

fun Track.hasAltitude(): Boolean {
    return !this.points.any { (it != null) && (!it.hasAltitude()) }
}

// has speed iff does not contain null elements and all points answer true to point.hasSpeed()
fun Track.hasSpeed(): Boolean {
    this.points.forEach {
        if(it != null){
            if(! it.hasSpeed()) return false
        } else {
            return false
        }
    }
    return true
}

// lap property
fun Track.hasAltitudeTotals(): Boolean {
    val allPoints = this.stats.hasElevationValues()
    val totals = this.stats.eleNegativeHeight < 1.0 && stats.elePositiveHeight > 1.0
    return allPoints && totals
}

// lap property
fun Track.hasAltitudeBounds(): Boolean {
    val allPoints = this.stats.hasElevationValues()
    val boundsVals = listOf(this.stats.altitudeMin, this.stats.altitudeMax)
    return boundsVals.all{it > -100 && it < 9000} && allPoints
}

// lap properties
fun Track.maxLat(): Double? = points.maxBy{it.latitude}?.latitude
fun Track.minLat(): Double? = points.minBy{it.latitude}?.latitude
fun Track.maxLon(): Double? = points.maxBy{it.longitude}?.longitude
fun Track.minLon(): Double? = points.minBy{it.longitude}?.longitude
