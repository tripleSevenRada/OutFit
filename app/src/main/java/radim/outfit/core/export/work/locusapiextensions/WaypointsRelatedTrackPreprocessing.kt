package radim.outfit.core.export.work.locusapiextensions

import android.util.Log
import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Location
import locus.api.objects.extra.Track
import locus.api.objects.utils.LocationCompute
import radim.outfit.DEBUG_MODE
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

class WaypointsRelatedTrackPreprocessing(private val track: Track) {

    private val tag = "TRACK_PREPROCESSING"

    fun preprocess(): Track {
        if (DEBUG_MODE) Log.i(tag, "call to preprocess")
        val needToConstructNewLocation =
                track.waypoints.filter { it.parameterRteAction == PointRteAction.UNDEFINED }
        return track
    }

    //TODO edge cases
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
    fun <T>linearInterpolatorGeneric(A: T, B: T, coef: Double):T where T: Number{
        return if (A is Double && B is Double) (A + ((B - A) * coef)) as T
        else if (A is Long && B is Long) (A + ((B - A) * coef).roundToLong()) as T
        else throw UnsupportedOperationException("linearInterpolatorGeneric")
    }

    fun interpolationCoef(A: Location, B: Location, C: Location): Double{
        val AB: Double = LocationCompute.computeDistanceFast(A, B)
        val AC: Double = LocationCompute.computeDistanceFast(A, C)
        return AC / AB
    }

    fun CIsWithinBounds(A: Location, B: Location, C: Location): Boolean {
        val latMin = min(A.latitude, B.latitude)
        val latMax = max(A.latitude, B.latitude)
        val lonMin = min(A.longitude, B.longitude)
        val lonMax = max(A.longitude, B.longitude)
        return (C.latitude in latMin..latMax && C.longitude in lonMin..lonMax)
    }
}
