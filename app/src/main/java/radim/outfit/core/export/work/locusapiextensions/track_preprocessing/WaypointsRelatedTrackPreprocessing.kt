package radim.outfit.core.export.work.locusapiextensions.track_preprocessing

import android.util.Log
import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Location
import locus.api.objects.extra.Point
import locus.api.objects.extra.Track
import locus.api.objects.utils.LocationCompute.computeDistanceFast
import radim.outfit.DEBUG_MODE
import radim.outfit.core.export.work.locusapiextensions.StarIterator
import radim.outfit.core.export.work.locusapiextensions.stringdumps.LocationStringDump.locationStringDescriptionSimple
import radim.outfit.core.export.work.locusapiextensions.stringdumps.PointStringDump
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.system.exitProcess

//https://drive.google.com/file/d/1CpRSCrSO8ARn8MOYh3YFkfZKGAPuex72/view?usp=sharing
//https://drive.google.com/file/d/1iZcHLD_r3CrN8K4fHi4bb9OzZlRUW2-t/view?usp=sharing
//https://drive.google.com/file/d/1KL5_sh6uKFXdsYyDFoQ9FoGFqixr5esT/view?usp=sharing
//https://drive.google.com/file/d/1zA38mtGoBME6RmqETP5W4RaMrq03wLy-/view?usp=sharing

fun getCurrentIndexOf(track: Track,
                      location: Location,
                      LocationToLastKnownIndex: MutableMap<Location, Int>): Int {
    val start = LocationToLastKnownIndex[location] ?: return -1
    if (track.points[start] === location) return start
    var count = 1
    while (true) {
        val rightInd = if (start + count <= track.points.lastIndex) start + count else return -1
        if (track.points[rightInd] === location) {
            LocationToLastKnownIndex[location] = rightInd; return rightInd
        }
        count++
    }
}

class WaypointsRelatedTrackPreprocessing(private val track: Track, private val debugMessages: MutableList<String>) {

    private val minDistConsider = 2.0
    private val debugInPreprocess = true
    private val tag = "WPTS preprocessing"
    private val howManyClustersExamine = 3

    fun preprocess(): TrackContainer {

        val n = 7 //how many closest locations to use as tree roots for insertion candidate

        val clusters = Clustering(debugInPreprocess).clusterize(track, debugMessages)

        // tests - as MutableLists
        // needToConstructNewLocation have paramRteIndex = -1
        val needToConstructNewLocation: List<Point> =
                track.waypoints.filter { it != null && it.parameterRteAction == PointRteAction.UNDEFINED }
        if (debugInPreprocess) {
            debugMessages.add("Need To Construct New Location: ")
            debugMessages.addAll(needToConstructNewLocation.map {
                PointStringDump.stringDescription(it)
                        .joinToString("\n")
            })
            needToConstructNewLocation.forEach {
                if (it.paramRteIndex != -1) {
                    Log.e(tag, "it.paramRteIndex != -1")
                    exitProcess(-1)
                }
            }
        }

        //memoization
        val lastKnownLocationToIndex = mutableMapOf<Location, Int>()
        var indCount = 0
        track.points.forEach { lastKnownLocationToIndex[it] = indCount; indCount++ }

        //
        //
        // we need to be able to provide indices of shifted trackpoints where
        // defined RteActions are now, after new trackpoints have been inserted
        val definedRteActionsToLocationsInTrack = mutableMapOf<Point, Location>()
        // rte actions waypoints have valid paramRteIndex (!= -1)
        val definedRteActions = track.waypoints.filter {
            it != null && it.parameterRteAction != PointRteAction.UNDEFINED // also PASS_PLACE
        }
        if (debugInPreprocess) {
            definedRteActions.forEach {
                if (it.paramRteIndex == -1) {
                    Log.e(tag, "it.paramRteIndex == -1 WHEN it.parameterRteAction != UNDEFINED")
                    exitProcess(-1)
                }
            }
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
            Log.e(tag, "definedRteActions.size != definedRteActionsToLocationsInTrack.size")
            exitProcess(-1)
        }
        //
        //

        // mocked stress test
        // needToConstructNewLocation.addAll(InjectTestWaypoints(track).getMockWaypointsWithinTrackBounds(100))

        val bagOfWpts = mutableSetOf<Point>()
        bagOfWpts.addAll(needToConstructNewLocation)

        needToConstructNewLocation.forEach {
            if (insertProjectedLocations(it.location, n, lastKnownLocationToIndex, clusters)) {
                bagOfWpts.remove(it)
            }
        }

        if (debugInPreprocess) {
            val debugMessage = "Not inserted WPTS - locations: ${bagOfWpts.size}"
            debugMessages.add(debugMessage)
            Log.i(tag, debugMessage)
            bagOfWpts.forEach { debugMessages.add(" -- ${locationStringDescriptionSimple(it.location)}\n") }

            val message1 = "HEURISTICS bagOfWpts.size: ${bagOfWpts.size}"
            debugMessages.add(message1)
            Log.w(tag, message1)
        }

        // apply HEURISTICS on remaining WPTS in bagOfWpts

        if (bagOfWpts.size > 0) {
            val starIt = StarIterator(bagOfWpts.first().location)
            val bagOfWptsCopy = mutableSetOf<Point>()
            bagOfWptsCopy.addAll(bagOfWpts)
            bagOfWptsCopy.forEach {
                starIt.reset(it.location)
                var inserted = 0
                for (i in 0 until 100) {
                    val movedLoc = starIt.next()
                    movedLoc ?: break
                    if (insertProjectedLocations(movedLoc, n, lastKnownLocationToIndex, clusters)) {
                        if (++inserted > 2) break
                    }
                }
                if (inserted > 0) bagOfWpts.remove(it)
            }
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
            // point.location and location in track (trackpoint) are not the same object,
            // although they have the same lat & lon
            val locSearched = definedRteActionsToLocationsInTrack[it]
            if (locSearched != null) {
                val currentIndex = getCurrentIndexOf(track, locSearched, lastKnownLocationToIndex)
                if (DEBUG_MODE && currentIndex == -1) {
                    Log.e(tag, "unexpected -1")
                    exitProcess(-1)
                }
                if(currentIndex != -1) definedRteActionsToShiftedIndices[it] = currentIndex
            } else {
                val debugMessage = "INCONSISTENCY IN :definedRteActionsToLocationsInTrack"
                Log.e(tag, debugMessage)
                if (debugInPreprocess) debugMessages.add(debugMessage)
            }
        }

        if (DEBUG_MODE) {
            if (definedRteActions.size != definedRteActionsToShiftedIndices.size) {
                Log.e(tag, "definedRteActions.size != definedRteActionsToShiftedIndices.size")
                exitProcess(-1)
            }
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

    private fun insertProjectedLocations(location: Location,
                                         n: Int,
                                         locationToLastKnownIndex: MutableMap<Location, Int>,
                                         clusters: MutableList<Cluster>
    ): Boolean {
        var inserted = false
        // list of n closest locations in track, sorted by distance to param location
        val closestLocations = getListOfClosestLocations(location, clusters, n)

        // TEST
        // val closestLocationsRef = getListOfClosestLocationsTestReference(location, n)
        // val same = compareLocationDistanceListTest(closestLocations, closestLocationsRef)

        val insertCandidates = mutableListOf<InsertCandidate>()

        // a function that tests if there are too close locations in insertCandidates already
        fun tooCloseToACandidateAlreadyIn(candidate: InsertCandidate): Boolean {
            val minDistCandidate = insertCandidates.minBy {
                computeDistanceFast(it.location, candidate.location)
            }
            minDistCandidate ?: return false
            return computeDistanceFast(minDistCandidate.location, candidate.location) < (minDistConsider / 2)
        }

        closestLocations.forEach {
            val root = getCurrentIndexOf(track, it.location, locationToLastKnownIndex)
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
                    // if there is not a too close location already
                    if (candidate != null &&
                            !tooCloseToACandidateAlreadyIn(candidate)) insertCandidates.add(candidate)
                }
                if (
                        tree.rightLoc != null &&
                        computeDistanceFast(tree.rightLoc, location) > minDistConsider
                ) {
                    val A = tree.closestLoc
                    val B = tree.rightLoc
                    val C = location
                    val candidate = getInsertCandidate(A, B, C,
                            "RIGHT branch of the closest tree", B)
                    // if there is not a too close location already
                    if (candidate != null &&
                            !tooCloseToACandidateAlreadyIn(candidate)) insertCandidates.add(candidate)
                }
            }
        } // closestLocations.forEach

        if (debugInPreprocess) {
            debugMessages.add("Insert Candidates size: ${insertCandidates.size}")
            debugMessages.add(insertCandidates.joinToString("\n"))
        }

        // we accept all candidates here, no filter except tooCloseToACandidateAlreadyIn
        insertCandidates.forEach {
            val currentIndex = getCurrentIndexOf(track, it.locationToReplace, locationToLastKnownIndex)
            if(currentIndex != -1) {
                track.points.add(currentIndex, it.location)
                locationToLastKnownIndex[it.location] = currentIndex
                val closestCluster = getClosestCluster(it.location, clusters)
                closestCluster?.members?.add(it.location)
                if (debugInPreprocess) Log.i(tag, "Inserted Candidate $it")
                inserted = true
            } else if (DEBUG_MODE) {
                Log.e(tag,"currentIndex == -1")
                exitProcess(-1)
            }
        }
        return inserted
    }

    private object DistanceProviderComparator : Comparator<DistanceProvider> {
        override fun compare(dp0: DistanceProvider, dp1: DistanceProvider): Int =
                dp0.getDistance().compareTo(dp1.getDistance())
    }

    private fun getListOfClosestLocations(
            point: Location,
            clusters: MutableList<Cluster>,
            n: Int): List<LocationDistance> {
        val clusterDistanceList = mutableListOf<ClusterDistance>()
        clusters.forEach { clusterDistanceList.add(ClusterDistance(it, computeDistanceFast(it.centroid, point))) }
        clusterDistanceList.sortWith(DistanceProviderComparator)

        val topClusterDistances = if (clusterDistanceList.size < howManyClustersExamine) clusterDistanceList
        else clusterDistanceList.subList(0, howManyClustersExamine)

        val closestLocationDistanceList = mutableListOf<LocationDistance>()
        topClusterDistances.flatMap { it.cluster.members }.forEach {
            closestLocationDistanceList.add(LocationDistance(it, computeDistanceFast(it, point)))
        }

        closestLocationDistanceList.sortWith(DistanceProviderComparator)

        return if (closestLocationDistanceList.size < n) closestLocationDistanceList
        else closestLocationDistanceList.subList(0, n)
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

    fun pointOnALineSegmentClosestToPoint(A: Location, B: Location, C: Location): Location {
        val t: Double =
                (((C.latitude - A.latitude) * (B.latitude - A.latitude)) + ((C.longitude - A.longitude) * (B.longitude - A.longitude))) /
                        (((B.latitude - A.latitude).pow(2)) + ((B.longitude - A.longitude).pow(2)))

        val lat = A.latitude + (t * (B.latitude - A.latitude))
        val lon = A.longitude + (t * (B.longitude - A.longitude))
        return Location(lat, lon)
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

    // STRESS TEST REFERENCE, does not use clusters
    private fun getListOfClosestLocationsTestReference(locationWpt: Location, n: Int): List<LocationDistance> {
        val locationDistanceList = mutableListOf<LocationDistance>()
        track.points.forEach { locationDistanceList.add(LocationDistance(it, computeDistanceFast(it, locationWpt))) }
        locationDistanceList.sortWith(DistanceProviderComparator)
        return if (locationDistanceList.size < n) locationDistanceList
        else locationDistanceList.subList(0, n)
    }

    // TEST COMPARE
    private fun compareLocationDistanceListTest(l1: List<LocationDistance>, l2: List<LocationDistance>): Boolean {
        Log.w(tag, (" - ${l1.hashCode()} - ${l1.hashCode()} ${l1 == l2}"))
        return l1 == l2
    }

}

interface DistanceProvider {
    fun getDistance(): Double
}

// we want to call computeDistanceFast(from: Location, to: Location) only n times
// not n*log(n) times during sorting
data class LocationDistance(val location: Location,
                            val distanceToPoint: Double) : DistanceProvider {
    override fun getDistance(): Double = distanceToPoint
}

data class ClusterDistance(val cluster: Cluster,
                           val distanceToPoint: Double) : DistanceProvider {
    override fun getDistance(): Double = distanceToPoint
}

data class TrackContainer(val track: Track,
                          val definedRteActionsToShiftedIndices: Map<Point, Int>,
                          var failedMessage: String = "",
                          var clusterize: Boolean = false,
                          var move: Boolean = false,
                          var moveDist: Double = 40.0)

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