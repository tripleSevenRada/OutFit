package radim.outfit.mocks

import android.util.Log
import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Location
import locus.api.objects.extra.Point
import locus.api.objects.extra.Track
import locus.api.objects.extra.TrackStats

fun getTrackOkNoCP(): Track {
    val t = rudimentaryTrack("mockNoCP")
    t.stats = TrackStats()
    t.stats.numOfPoints = t.points.size
    return t
}

fun getTrackNullEndNoCP(): Track {
    val t = rudimentaryTrack("mockNullEndNoCP")
    t.points.add(null)
    t.stats = TrackStats()
    t.stats.numOfPoints = t.points.size
    return t
}

fun getTrackNullStartNoCP(): Track {
    val t = rudimentaryTrack("mockNullStartNoCP")
    t.points.add(0,null)
    t.stats = TrackStats()
    t.stats.numOfPoints = t.points.size
    return t
}

fun getTrackRandomNullsNoCP(): Track {
    val t = rudimentaryTrack("mockRandomNullsNoCP")
    t.points.add(0,null)
    t.points.add(7,null)
    t.points.add(4,null)
    t.points.add(null)
    t.stats = TrackStats()
    t.stats.numOfPoints = t.points.size
    return t
}

fun rudimentaryTrack(name: String = "some_mock"): Track {
    val t = Track()
    t.name = name
    val trackPoints = mutableListOf<Location>()
    val incr = 0.005
    var count = 0
    repeat(10) {
        val l = Location()
        l.longitude = 14.000 + (count * incr)
        l.latitude = 50.000 + (count * incr)
        trackPoints.add(l)
        count++
        Log.i("MOCKS", "${l.longitude} | ${l.latitude}")
    }
    t.points = trackPoints
    return t
}
