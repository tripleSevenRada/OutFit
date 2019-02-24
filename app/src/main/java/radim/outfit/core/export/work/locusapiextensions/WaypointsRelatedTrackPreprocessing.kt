package radim.outfit.core.export.work.locusapiextensions

import android.util.Log
import locus.api.objects.extra.Location
import locus.api.objects.extra.Track
import radim.outfit.DEBUG_MODE
import kotlin.math.pow
import kotlin.math.roundToLong

class WaypointsRelatedTrackPreprocessing(private val track: Track) {

    private val tag = "TRACK_PREPROCESSING"

    fun preprocess(): Track {
        if (DEBUG_MODE) Log.i(tag, "call to preprocess")
        //TODO
        val trackPoints: MutableList<Location> = track.points

        val testLocFrom = trackPoints[0]
        val testLocInBetween = trackPoints[1]
        val testLocTo = trackPoints[2]
        val coef = interpolationCoef(testLocFrom, testLocTo, testLocInBetween)

        val doubleInterpolated: Double = linearInterpolatorGeneric(13.5, 16.4, coef)
        Log.i(tag, "doubleInterpolated: $doubleInterpolated")

        val longInterpolated: Long = linearInterpolatorGeneric(135L, 164L, coef)
        Log.i(tag, "longInterpolated: $longInterpolated")

        return track
    }







    fun pointOnALineSegmentClosestToPoint(A: Location, B: Location, C: Location): Location {
        val t: Double =
                ((C.latitude - A.latitude) * (B.latitude - A.latitude) + (C.longitude - A.longitude) * (B.longitude - A.longitude)) /
                        ((B.latitude - A.latitude).pow(2) + (B.longitude - A.longitude).pow(2))
        return Location(A.latitude + (t * (B.latitude - A.latitude)), A.longitude + (t * (B.longitude - A.longitude)))
    }

    // https://discuss.kotlinlang.org/t/how-to-write-generic-functions-for-all-numeric-types/7367
    // There’s no completely satisfactory way to write generic functions for all numeric types.
    // val double = linearInterpolatorGeneric(1.0, 2.0, 0.5)
    @Suppress("Unchecked_cast")
    fun <T>linearInterpolatorGeneric(from: T, to: T, coef: Double):T where T: Number{
        return if (from is Double && to is Double) (from + ((to - from) * coef)) as T
        else if (from is Long && to is Long) (from + ((to - from) * coef).roundToLong()) as T
        else throw UnsupportedOperationException("linearInterpolatorGeneric - UnsupportedOperationException")
    }

    fun interpolationCoef(from: Location, to: Location, inBetween: Location): Double{
        //TODO
        return 0.5
    }



}
