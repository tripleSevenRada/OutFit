package radim.outfit.core.export.work.locusapiextensions.track_preprocessing

import android.util.Log
import locus.api.objects.extra.Location
import locus.api.objects.extra.Track
import locus.api.objects.utils.LocationCompute.computeDistanceFast

class Clustering(val debug: Boolean) {

    private val tag = "CLUSTERING"
    private val clustersSize = 60

    fun clusterize(track: Track): List<Cluster> {
        // k-clusters
        val k = (track.points.size / clustersSize) + 1
        val clusters = mutableListOf<Cluster>()
        val locationsCentroids = mutableSetOf<Location>()
        val locationToDistFromSelectedCentroids = mutableMapOf<Location, Double>()
        track.points.forEach { if (it != null) locationToDistFromSelectedCentroids[it] = Double.MAX_VALUE }

        fun addToClusters(centroid: Location) {
            clusters.add(Cluster(centroid, mutableListOf()))
            locationsCentroids.add(centroid)
            if (debug) Log.i(tag, "ADDING $centroid")
        }

        // first
        val middle = track.points.size / 2
        
        // TODO null safety
        var latestCentroid: Location = track.points[middle]

        addToClusters(latestCentroid)

        if (debug) Log.i(tag, "We want $k clusters from ${track.points.size} elements.")

        for (i in 1 until k) {
            val farthestLocation = getFarthestLocation(
                    track,
                    latestCentroid,
                    locationToDistFromSelectedCentroids,
                    locationsCentroids)

            latestCentroid = farthestLocation
            addToClusters(latestCentroid)

        }

        if (debug) Log.i(tag, "Size: ${clusters.size}")

        return clusters
    }

    fun getClosestLocations(n: Int): List<Location> {
        TODO()
    }

    // impl
    private fun getFarthestLocation(track: Track,
                                    latestCentroid: Location,
                                    locationToDistFromSelectedCentroids: MutableMap<Location, Double>,
                                    locationsCentroids: Set<Location>): Location {
        // only updates locationToDistFromSelectedCentroids
        var farthest = latestCentroid
        var maxDist = 0.0
        track.points.forEach { current ->
            if (current != null && !locationsCentroids.contains(current)) {

                // first update (farthest first traversal)
                // For each remaining not-yet-selected NodeEntity n, replace the distance stored
                // for n by the minimum of its oldDist and the distance from latestCentroid to n.
                val oldDist = locationToDistFromSelectedCentroids[current] ?: Double.MAX_VALUE
                val update = Math.min(oldDist, computeDistanceFast(latestCentroid, current))
                locationToDistFromSelectedCentroids[current] = update

                // Scan the list of not-yet-selected NodeEntities to find a NodeEntity farthest, that
                // has the maximum distance from the selected Centroids (Locations)
                if (update > maxDist) {
                    maxDist = update
                    farthest = current
                }
            }
        }
        return farthest
    }
}

data class Cluster(val centroid: Location, val members: MutableList<Location>)