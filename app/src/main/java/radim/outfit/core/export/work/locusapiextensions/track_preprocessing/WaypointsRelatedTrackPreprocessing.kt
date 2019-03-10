package radim.outfit.core.export.work.locusapiextensions.track_preprocessing

import android.util.Log
import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Location
import locus.api.objects.extra.Point
import locus.api.objects.extra.Track
import locus.api.objects.utils.LocationCompute.computeDistanceFast
import radim.outfit.DEBUG_MODE
import radim.outfit.core.export.work.MAX_DISTANCE_TO_CLIP_WP_TO_COURSE
import radim.outfit.core.export.work.locusapiextensions.StarIterator
import radim.outfit.core.export.work.locusapiextensions.stringdumps.LocationStringDump.locationStringDescriptionSimple
import radim.outfit.core.export.work.locusapiextensions.stringdumps.PointStringDump
import java.lang.RuntimeException
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
    private val debugInPreprocess = true
    private val tag = "WPTS preprocessing"

    fun preprocess(): TrackContainer {

        // needToConstructNewLocation have paramRteIndex = -1
        val needToConstructNewLocation: List<Point> =
                track.waypoints.filter { it != null && it.parameterRteAction == PointRteAction.UNDEFINED }
        if (debugInPreprocess) {
            debugMessages.add("Need To Construct New Location: ")
            debugMessages.addAll(needToConstructNewLocation.map {
                PointStringDump.stringDescription(it)
                        .joinToString("\n")
            })
        }

        //memoization
        val lastKnownLocationToIndex = mutableMapOf<Location, Int>()
        var indCount = 0
        track.points.forEach { lastKnownLocationToIndex[it] = indCount; indCount++ }

        //
        //
        // we need to be able to provide indices of shifted trackpoints where
        // defined RteActions are now, after new trackpoints were inserted
        val definedRteActionsToLocationsInTrack = mutableMapOf<Point, Location>()
        // rte actions waypoints have valid paramRteIndex (!= -1)
        val definedRteActions = track.waypoints.filter {
            it != null && it.paramRteIndex != -1 &&
                    it.parameterRteAction != PointRteAction.UNDEFINED
        }
        definedRteActions.forEach {
            if (it.paramRteIndex in track.points.indices) {
                definedRteActionsToLocationsInTrack[it] = track.points[it.paramRteIndex]
            } else {
                val debugMessage = "ERROR: paramRteIndex"
                Log.e(tag, debugMessage)
                if (debugInPreprocess) debugMessages.add(debugMessage)
            }
        }
        if (DEBUG_MODE && definedRteActions.size != definedRteActionsToLocationsInTrack.size) {
            throw RuntimeException("definedRteActions.size != definedRteActionsToLocationsInTrack.size")
        }
        //
        //

        // mocked stress test
        // needToConstructNewLocation.addAll(InjectTestWaypoints(track).getMockWaypointsWithinTrackBounds(10000))

        val bagOfWpts = mutableSetOf<Point>()
        bagOfWpts.addAll(needToConstructNewLocation)

        val n = 7 //how many closest locations to use as tree roots for insertion candidate

        needToConstructNewLocation.forEach {
            if (insertProjectedLocations(it.location, n, lastKnownLocationToIndex)) {
                bagOfWpts.remove(it)
            }
        }

        if (debugInPreprocess) {
            val debugMessage = "Not inserted WPTS - locations: ${bagOfWpts.size}"
            debugMessages.add(debugMessage)
            Log.i(tag, debugMessage)
            bagOfWpts.forEach { debugMessages.add(" -- ${locationStringDescriptionSimple(it.location)}\n") }
        }

        // apply HEURISTICS on remaining WPTS in bagOfWpts

        if (debugInPreprocess) {
            val message1 = "HEURISTICS bagOfWpts.size: ${bagOfWpts.size}"
            debugMessages.add(message1)
            Log.w(tag, message1)
        }

        val bagOfWptsCopy = mutableSetOf<Point>()
        bagOfWptsCopy.addAll(bagOfWpts)
        bagOfWptsCopy.forEach {
            val starIt = StarIterator(it.location)
            var inserted = 0
            for (i in 0..80) {
                val movedLoc = starIt.next()
                movedLoc ?: break
                if (computeDistanceFast(it.location, movedLoc) > MAX_DISTANCE_TO_CLIP_WP_TO_COURSE * 5) break
                if (insertProjectedLocations(movedLoc, n, lastKnownLocationToIndex)) {
                    if (++inserted > 2) {
                        if (debugInPreprocess) Log.i(tag, "breaking @ $inserted")
                        break
                    }
                }
                if (debugInPreprocess) Log.i(tag, " iter. = $i")
            }
            if (inserted > 0) bagOfWpts.remove(it)
        }

        if (debugInPreprocess) {
            val message1 = "AFTER HEURISTICS bagOfWpts.size: ${bagOfWpts.size}"
            debugMessages.add(message1)
            Log.w(tag, message1)
            bagOfWpts.forEach {
                val message2 = " -- ${locationStringDescriptionSimple(it.location)}"
                debugMessages.add(message2)
                Log.w(tag, message2)
            }
        }

        val definedRteActionsToShiftedIndices = mutableMapOf<Point, Int>()
        definedRteActions.forEach {
            // point.location and location in trackContainer are not the same object,
            // although they have the same lat & lon
            val locSearched = definedRteActionsToLocationsInTrack[it]
            if (locSearched != null) {
                definedRteActionsToShiftedIndices[it] = getCurrentIndexOf(locSearched, lastKnownLocationToIndex)
            } else {
                val debugMessage = "INCONSISTENCY IN :definedRteActionsToLocationsInTrack"
                Log.e(tag, debugMessage)
                if (debugInPreprocess) debugMessages.add(debugMessage)
            }
        }
        if (DEBUG_MODE) {
            if (definedRteActions.size != definedRteActionsToShiftedIndices.size)
                throw RuntimeException("definedRteActions.size != definedRteActionsToShiftedIndices.size")
            debugMessages.add("definedRteActionsToLocationsInTrack before processing +++++++++++++++++++")
            definedRteActionsToLocationsInTrack.forEach {
                debugMessages.add(" location -- ${locationStringDescriptionSimple(it.value)}")
            }
            debugMessages.add("size definedRteActions: ${definedRteActions.size}")
            debugMessages.add("size definedRteActionsToLocationsInTrack: ${definedRteActionsToLocationsInTrack.size}")
            debugMessages.add("size definedRteActionsToShiftedIndices: ${definedRteActionsToShiftedIndices.size}")
            debugMessages.add("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")
        }

        return TrackContainer(track, definedRteActionsToShiftedIndices)
    }

    //TODO Big O
    private fun insertProjectedLocations(location: Location, n: Int, locationToLastKnownIndex: MutableMap<Location, Int>): Boolean {
        var inserted = false
        // list of n closest locations in track, sorted by distance to param location
        val closestLocations = getListOfClosestLocations(location, n)
        val insertCandidates = mutableListOf<InsertCandidate>()

        closestLocations.forEach {
            val root = getCurrentIndexOf(it.location, locationToLastKnownIndex)
            if (root != -1) {
                if (debugInPreprocess) {
                    debugMessages.add("\n\n\n\n\nPOINT--------------------------------------------------")
                    debugMessages.add(locationStringDescriptionSimple(location))
                    debugMessages.add("distance to closest: ${computeDistanceFast(location, track.points[root])}")
                    debugMessages.add("closest: ${locationStringDescriptionSimple(track.points[root])}")
                }

                // START tree
                val indexLeft = if (root > 0 && track.points[root - 1] != null)
                    root - 1 else -1
                val indexRight = if (root < (track.points.size - 1) && track.points[root + 1] != null)
                    root + 1 else -1
                val tree = ClosestTree(track.points[root],
                        if (indexLeft != -1) track.points[indexLeft] else null,
                        if (indexRight != -1) track.points[indexRight] else null)
                // END tree

                if (debugInPreprocess) {
                    debugMessages.add("TREE------------------------")
                    debugMessages.add(tree.toString())
                    debugMessages.add("----------------------------")
                }
                if (
                        tree.leftLoc != null &&
                        computeDistanceFast(tree.leftLoc, location) > minDistConsider
                ) {
                    val A = tree.closestLoc
                    val B = tree.leftLoc
                    val C = location
                    val candidate = getInsertCandidate(A, B, C,
                            "LEFT branch of the closest tree", A)
                    if (candidate != null) insertCandidates.add(candidate)
                }
                if (
                        tree.rightLoc != null &&
                        computeDistanceFast(tree.rightLoc,location) > minDistConsider
                ) {
                    val A = tree.closestLoc
                    val B = tree.rightLoc
                    val C = location
                    val candidate = getInsertCandidate(A, B, C,
                            "RIGHT branch of the closest tree", B)
                    if (candidate != null) insertCandidates.add(candidate)
                }
            }
        } // closestLocations.forEach

        if (debugInPreprocess) {
            debugMessages.add("Insert Candidates size: ${insertCandidates.size}")
            debugMessages.add(insertCandidates.joinToString("\n"))
        }

        // we accept all candidates, no filter
        insertCandidates.forEach {
            val currentIndex = getCurrentIndexOf(it.locationToReplace, locationToLastKnownIndex)
            track.points.add(currentIndex, it.location)
            locationToLastKnownIndex[it.location] = currentIndex
            if (debugInPreprocess) Log.i(tag, "Inserted Candidate $it")
            inserted = true
        }
        return inserted
    }

    private object LocationDistanceComparator : Comparator<LocationDistance> {
        override fun compare(o1: LocationDistance, o2: LocationDistance): Int =
                o1.distanceToPoint.compareTo(o2.distanceToPoint)
    }

    // not used
    private object InsertCandidatesComparator : Comparator<InsertCandidate> {
        override fun compare(c1: InsertCandidate, c2: InsertCandidate): Int =
                c1.distanceToPoint.compareTo(c2.distanceToPoint)
    }

    private fun getListOfClosestLocations(waypoint: Location, n: Int): List<LocationDistance> {
        val locationDistanceList = mutableListOf<LocationDistance>()
        for (i in track.points.indices) {
            if (track.points[i] != null) {
                locationDistanceList.add(
                        LocationDistance(track.points[i],
                                computeDistanceFast(track.points[i], waypoint)
                        ))
            }
        }
        locationDistanceList.sortWith(LocationDistanceComparator)
        return if (n < locationDistanceList.size)
            locationDistanceList.subList(0, n)
        else locationDistanceList
    }

    private fun getInsertCandidate(A: Location, B: Location, C: Location,
                                   desc: String, locationToReplace: Location): InsertCandidate? {
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
                val candidate = InsertCandidate(D, computeDistanceFast(D, C).toDouble(), locationToReplace)
                if (debugInPreprocess) debugMessages.add("$candidate")
                return candidate
            }
        }
        return null
    }

    private fun Location.isNotTooCloseTo(A: Location, B: Location): Boolean =
            computeDistanceFast(this, A) > minDistConsider / 2 &&
                    computeDistanceFast(this, B) > minDistConsider / 2

    private fun getCurrentIndexOf(location: Location,
                                  LocationToLastKnownIndex: MutableMap<Location, Int>): Int {
        val start = LocationToLastKnownIndex[location]?: return -1
        if(track.points[start] === location) return start
        var count = 1
        while(true){
            val rightInd = if(start + count <= track.points.lastIndex) start + count else return -1
            if(track.points[rightInd] === location) {
                LocationToLastKnownIndex[location] = rightInd; return rightInd
            }
            count ++
        }
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
        val AB: Double = computeDistanceFast(A, B)
        val AC: Double = computeDistanceFast(A, C)
        if (AC < (minDistConsider / 2) || AB < (minDistConsider / 2)) return -1.0
        return AC / AB
    }

    private fun Location.isWithinBounds(A: Location, B: Location): Boolean {
        val latMin = min(A.latitude, B.latitude)
        val latMax = max(A.latitude, B.latitude)
        val lonMin = min(A.longitude, B.longitude)
        val lonMax = max(A.longitude, B.longitude)
        return (this.latitude in latMin..latMax && this.longitude in lonMin..lonMax)
    }

    private data class ClosestTree(val closestLoc: Location,
                                   val leftLoc: Location?,
                                   val rightLoc: Location?) {
        override fun toString(): String {
            return "ClosestTree:\n" +
                    "closest: ${locationStringDescriptionSimple(closestLoc)}\n" +
                    "left: ${locationStringDescriptionSimple(leftLoc)}\n" +
                    "right: ${locationStringDescriptionSimple(rightLoc)}\n"
        }
    }

    private data class InsertCandidate(val location: Location,
                                       val distanceToPoint: Double,
                                       val locationToReplace: Location) {
        override fun toString(): String {
            return "InsertCandidate:\n" +
                    "location: ${locationStringDescriptionSimple(location)}\n" +
                    "distanceToPoint: $distanceToPoint\n"
        }
    }
}

data class LocationDistance(val location: Location,
                            val distanceToPoint: Double)
data class TrackContainer(val track: Track, val definedRteActionsToShiftedIndices: Map<Point, Int>)

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