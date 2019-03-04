package radim.outfit.core.export.work.locusapiextensions

import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Location
import locus.api.objects.extra.Point
import locus.api.objects.extra.Track
import radim.outfit.core.export.work.locusapiextensions.stringdumps.LocationStringDump.locationStringDescriptionSimple
import radim.outfit.core.export.work.locusapiextensions.stringdumps.PointStringDump
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

//https://drive.google.com/file/d/1CpRSCrSO8ARn8MOYh3YFkfZKGAPuex72/view?usp=sharing
//https://drive.google.com/file/d/1iZcHLD_r3CrN8K4fHi4bb9OzZlRUW2-t/view?usp=sharing
//https://drive.google.com/file/d/1KL5_sh6uKFXdsYyDFoQ9FoGFqixr5esT/view?usp=sharing
//https://drive.google.com/file/d/1zA38mtGoBME6RmqETP5W4RaMrq03wLy-/view?usp=sharing

class WaypointsRelatedTrackPreprocessing(private val track: Track, private val debugMessages: MutableList<String>) {

    private val minDistConsider = 2.0
    private val debugInPreprocess = false

    fun preprocess(): Track {
        val needToConstructNewLocation: List<Point> =
                track.waypoints.filter { it.parameterRteAction == PointRteAction.UNDEFINED }

        if (debugInPreprocess) {
            debugMessages.add("Need To Construct New Location: ")
            debugMessages.addAll(needToConstructNewLocation.map {
                PointStringDump.stringDescription(it)
                        .joinToString("\n")
            })
        }

        // mocked stress test
        // needToConstructNewLocation.addAll(InjectTestWaypoints(track).getMockWaypointsWithinTrackBounds(10000))

        val bagOfWpts = mutableSetOf<Point>()
        bagOfWpts.addAll(needToConstructNewLocation)

        needToConstructNewLocation.forEach {
            if (insertProjectedLocation(it.location)) bagOfWpts.remove(it)
        }

        if (debugInPreprocess) {
            debugMessages.add("Not inserted WPTS - locations: ${bagOfWpts.size}")
            bagOfWpts.forEach { debugMessages.add(locationStringDescriptionSimple(it.location)) }
        }
        // apply HEURISTICS on remaining WPTS in bagOfWpts

        return track
    }

    private fun insertProjectedLocation(location: Location): Boolean {
        var inserted = false
        val indexClosestLoc = getClosestLocationIndex(location)
        val insertCandidates = mutableListOf<InsertCandidate>()
        if (indexClosestLoc != -1) {
            if (debugInPreprocess) {
                debugMessages.add("\n\n\n\n\nPOINT--------------------------------------------------")
                debugMessages.add(locationStringDescriptionSimple(location))
                debugMessages.add("distance to closest: ${location.distanceTo(track.points[indexClosestLoc])}")
                debugMessages.add("closest: ${locationStringDescriptionSimple(track.points[indexClosestLoc])}")
            }
            // START tree
            val indexLeft = if (indexClosestLoc > 0 && track.points[indexClosestLoc - 1] != null)
                indexClosestLoc - 1 else -1
            val indexRight = if (indexClosestLoc < (track.points.size - 1) && track.points[indexClosestLoc + 1] != null)
                indexClosestLoc + 1 else -1
            val tree = ClosestTree(indexClosestLoc, track.points[indexClosestLoc],
                    indexLeft, if (indexLeft != -1) track.points[indexLeft] else null,
                    indexRight, if (indexRight != -1) track.points[indexRight] else null)
            // END tree
            if (debugInPreprocess) {
                debugMessages.add("TREE------------------------")
                debugMessages.add(tree.toString())
                debugMessages.add("----------------------------")
            }
            if (
                    tree.leftInd != -1 &&
                    tree.leftLoc != null &&
                    tree.leftLoc.distanceTo(location) > minDistConsider
            ) {
                val A = tree.closestLoc
                val B = tree.leftLoc
                val C = location
                val candidate = getInsertCandidate(A, B, C, tree.closestInd,
                        "LEFT branch of the closest tree")
                if (candidate != null) insertCandidates.add(candidate)
            }
            if (
                    tree.rightInd != -1 &&
                    tree.rightLoc != null &&
                    tree.rightLoc.distanceTo(location) > minDistConsider
            ) {
                val A = tree.closestLoc
                val B = tree.rightLoc
                val C = location
                val candidate = getInsertCandidate(A, B, C, tree.rightInd,
                        "RIGHT branch of the closest tree")
                if (candidate != null) insertCandidates.add(candidate)
            }
        }
        val winnerCandidate = insertCandidates.minWith(InsertCandidatesComparator)
        if (debugInPreprocess) {
            debugMessages.add("Insert Candidates size: ${insertCandidates.size}")
            debugMessages.add("Candidates: $insertCandidates")
            debugMessages.add("Winner candidate: ${winnerCandidate?.toString()}\n\n")
        }
        if (winnerCandidate != null) {
            track.points.add(winnerCandidate.indexOfInsert, winnerCandidate.location)
            inserted = true
        }
        return inserted
    }

    private object InsertCandidatesComparator : Comparator<InsertCandidate> {
        override fun compare(c1: InsertCandidate, c2: InsertCandidate): Int =
                c1.distanceToPoint.compareTo(c2.distanceToPoint)
    }

    private fun getInsertCandidate(A: Location, B: Location, C: Location, insertIndex:
    Int, desc: String): InsertCandidate? {
        val D = pointOnALineSegmentClosestToPoint(A, B, C)
        if (D.isWithinBounds(A, B) && D.isNotTooCloseTo(A, B)) {
            // D isWithinBounds ON LEFT OR RIGHT
            // perform interpolations A(closest) -> D(new one) -> B(left or right)
            val coef = interpolationCoef(A, B, D)
            if (coef > 0.0 && coef < 1.0) {
                if (debugInPreprocess) debugMessages.add("$desc interpolationCoef(A,B,D) = $coef")
                if (A.hasAltitude() && B.hasAltitude()) {
                    D.altitude = linearInterpolatorGeneric(A.altitude, B.altitude, coef)
                    if (debugInPreprocess) {
                        debugMessages.add("A.altitude: ${A.altitude}")
                        debugMessages.add("B.altitude: ${B.altitude}")
                        debugMessages.add("Interpolation result: D.altitude: ${D.altitude}")
                        debugMessages.add("Back check: D.hasAltitude: ${D.hasAltitude()}\n")
                    }
                }
                if (A.time > 100L && B.time > 100L) {
                    D.time = linearInterpolatorGeneric(A.time, B.time, coef)
                    if (debugInPreprocess) {
                        debugMessages.add("A.time: ${A.time}")
                        debugMessages.add("B.time: ${B.time}")
                        debugMessages.add("Interpolation result: D.time: ${D.time}\n")
                    }
                } else if (debugInPreprocess) debugMessages.add("no timestamps to interpolate\n")
                val candidate = InsertCandidate(D, D.distanceTo(C).toDouble(), insertIndex)
                if (debugInPreprocess) debugMessages.add("$candidate")
                return candidate
            }
        }
        return null
    }

    private fun Location.isNotTooCloseTo(A: Location, B: Location): Boolean =
            this.distanceTo(A) > minDistConsider / 2 &&
                    this.distanceTo(B) > minDistConsider / 2

    private fun getClosestLocationIndex(location: Location): Int {
        var closest = -1
        var closestDist = Float.MAX_VALUE
        for (i in track.points.indices) {
            if (track.points[i] == null) continue
            val dist = track.points[i].distanceTo(location)
            if (dist < closestDist) {
                closestDist = dist
                closest = i
            }
        }
        return closest
    }

    fun pointOnALineSegmentClosestToPoint(A: Location, B: Location, C: Location): Location {
        val t: Double =
                (((C.latitude - A.latitude) * (B.latitude - A.latitude)) + ((C.longitude - A.longitude) * (B.longitude - A.longitude))) /
                        (((B.latitude - A.latitude).pow(2)) + ((B.longitude - A.longitude).pow(2)))

        val lat = A.latitude + (t * (B.latitude - A.latitude))
        val lon = A.longitude + (t * (B.longitude - A.longitude))
        return Location(lat, lon)
    }

    // https://discuss.kotlinlang.org/t/how-to-write-generic-functions-for-all-numeric-types/7367
    // There’s no completely satisfactory way to write generic functions for all numeric types.
    // val double = linearInterpolatorGeneric(1.0, 2.0, 0.5)
    @Suppress("Unchecked_cast")
    private fun <T> linearInterpolatorGeneric(A: T, B: T, coef: Double): T where T : Number {
        return if (A is Double && B is Double) (A + ((B - A) * coef)) as T
        else if (A is Long && B is Long) (A + ((B - A) * coef).roundToLong()) as T
        else throw UnsupportedOperationException("linearInterpolatorGeneric")
    }

    fun interpolationCoef(A: Location, B: Location, C: Location): Double {
        val AB: Double = A.distanceTo(B).toDouble()
        val AC: Double = A.distanceTo(C).toDouble()
        if (AC < (minDistConsider / 2) || AB < (minDistConsider / 2)) return -1.0
        return AC / AB
    }

    private fun Location.isWithinBounds(A: Location, B: Location): Boolean {
        if (debugInPreprocess) {
            debugMessages.add("isWithinBounds:")
            debugMessages.add("A ${locationStringDescriptionSimple(A)}")
            debugMessages.add("B ${locationStringDescriptionSimple(B)}")
            debugMessages.add("this ${locationStringDescriptionSimple(this)}")
        }
        val latMin = min(A.latitude, B.latitude)
        val latMax = max(A.latitude, B.latitude)
        val lonMin = min(A.longitude, B.longitude)
        val lonMax = max(A.longitude, B.longitude)
        return (this.latitude in latMin..latMax && this.longitude in lonMin..lonMax)
    }

    private data class ClosestTree(val closestInd: Int, val closestLoc: Location,
                                   val leftInd: Int, val leftLoc: Location?,
                                   val rightInd: Int, val rightLoc: Location?) {
        override fun toString(): String {
            return "ClosestTree:\n" +
                    "closest: $closestInd ${locationStringDescriptionSimple(closestLoc)}\n" +
                    "left: $leftInd ${locationStringDescriptionSimple(leftLoc)}\n" +
                    "right: $rightInd ${locationStringDescriptionSimple(rightLoc)}\n"
        }
    }

    private data class InsertCandidate(val location: Location,
                                       val distanceToPoint: Double,
                                       val indexOfInsert: Int) {
        override fun toString(): String {
            return "InsertCandidate:\n" +
                    "location: ${locationStringDescriptionSimple(location)}\n" +
                    "distanceToPoint: $distanceToPoint\n" +
                    "indexOfInsert: $indexOfInsert\n"
        }
    }
}

// mocked stress test
class InjectTestWaypoints(val track: Track) {
    fun getMockWaypointsWithinTrackBounds(howMany: Int): List<Point> {
        val maxLat: Location? = track.points.maxBy { it.latitude }
        val minLat: Location? = track.points.minBy { it.latitude }
        val maxLon: Location? = track.points.maxBy { it.longitude }
        val minLon: Location? = track.points.minBy { it.longitude }

        val maxLatD: Double = maxLat?.latitude ?: 0.0
        val minLatD: Double = minLat?.latitude ?: 0.0
        val maxLonD: Double = maxLon?.longitude ?: 0.0
        val minLonD: Double = minLon?.longitude ?: 0.0

        val deltaLat = maxLatD - minLatD
        val deltaLon = maxLonD - minLonD

        val mocks = mutableListOf<Point>()
        repeat(howMany) {
            val coefLat = Math.random()
            val coefLon = Math.random()
            val loc = Location()
            loc.latitude = minLatD + (coefLat * deltaLat)
            loc.longitude = minLonD + (coefLon * deltaLon)
            loc.altitude = 235.5
            val p = Point()
            p.location = loc
            p.parameterRteAction = PointRteAction.UNDEFINED
            mocks.add(p)
        }
        return mocks
    }
}