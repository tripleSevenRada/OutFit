package radim.outfit.core.export.work.locusapiextensions.track_preprocessing

import android.util.Log
import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Point
import locus.api.objects.extra.Track
import locus.api.objects.utils.LocationCompute.computeDistanceFast
import radim.outfit.DEBUG_MODE
import radim.outfit.core.export.work.locusapiextensions.allLeft
import radim.outfit.core.export.work.locusapiextensions.allRight
import radim.outfit.core.export.work.locusapiextensions.allStraight
import kotlin.system.exitProcess

class Kruskal(val debugMessages: MutableList<String>) {

    private val tag = "Kruskal"
    private val edges = mutableListOf<Edge>()
    private val forest = mutableListOf<MutableList<Int>>()
    private lateinit var rteActionsOnlyWP: List<Point>

    fun clusterize(minDistWP: Double, trackContainer: TrackContainer, nameReplacement: String): TrackContainer {

        Log.e(tag, "KRUSKAL CLUSTERIZE")

        val track = trackContainer.track
        val actionsToIndices: Map<Point, Int> = trackContainer.definedRteActionsToShiftedIndices

        if (DEBUG_MODE) {
            debugMessages.add("KRUSKAL CLUSTERIZE waypoints before preprocess")
            track.waypoints.forEach { debugMessages.add(it.parameterRteAction.toString()) }
        }

        preprocess(track, actionsToIndices)

        for (i in edges.indices) {
            if (edges[i].dist > minDistWP) break
            val left = getFirstLeftNonEmptyListOrThis(edges[i].leftInd)
            if (DEBUG_MODE && left == -1) {
                Log.e(tag,"left == -1")
                exitProcess(-1)
            }
            if (DEBUG_MODE && forest[edges[i].rightInd].isEmpty()) {
                Log.e(tag, "right is empty")
                exitProcess(-1)
            }
            forest[left].addAll(forest[edges[i].rightInd])
            forest[edges[i].rightInd].clear()
        }

        if (DEBUG_MODE) {
            debugMessages.add("KRUSKAL CLUSTERIZE after clustering lists")
            forest.forEach {
                debugMessages.add(it.size.toString())
                debugMessages.add(it.joinToString { index -> "$index, " } + "\n")
            }
        }

        val survivorsRteActions = mutableSetOf<Point>()
        forest.forEach { cluster ->
            if (cluster.size > 0) survivorsRteActions.add(rteActionsOnlyWP[cluster[0]])
            if (cluster.size > 1) {
                with(rteActionsOnlyWP[cluster[0]]) {
                    parameterRteAction =
                            if (cluster.all {
                                        allLeft.contains(rteActionsOnlyWP[it].parameterRteAction)
                                    }) PointRteAction.LEFT
                            else if (cluster.all {
                                        allRight.contains(rteActionsOnlyWP[it].parameterRteAction)
                                    }) PointRteAction.RIGHT
                            else if (cluster.all {
                                        allStraight.contains(rteActionsOnlyWP[it].parameterRteAction)
                                    }) PointRteAction.CONTINUE_STRAIGHT
                            else PointRteAction.NO_MANEUVER
                    name = nameReplacement
                }
            }
        }
        val finalWPTList = track.waypoints.filter {
            survivorsRteActions.contains(it) ||
                    it.parameterRteAction == PointRteAction.UNDEFINED ||
                    it.parameterRteAction == PointRteAction.PASS_PLACE
        }
        track.waypoints.clear()
        track.waypoints.addAll(finalWPTList)
        return trackContainer
    }

    private fun getFirstLeftNonEmptyListOrThis(index: Int): Int {
        if (forest[index].isNotEmpty()) return index
        var indexCopy = index
        while (indexCopy >= 0) {
            if (forest[indexCopy].isNotEmpty()) return indexCopy
            indexCopy--
        }
        return if (DEBUG_MODE) -1 else index
    }

    private fun preprocess(track: Track, actionsToRteIndices: Map<Point, Int>) {

        fun getDistBtwWpts(rteIndLeft: Int, rteIndRight: Int): Double {
            var accumulator = 0.0
            for (i in rteIndLeft until rteIndRight) {
                if (track.getPoint(i) != null && track.getPoint(i + 1) != null)
                    accumulator += computeDistanceFast(track.getPoint(i), track.getPoint(i + 1))
            }
            return accumulator
        }

        // NOT PointRteAction.UNDEFINED; NOT PointRteAction.PASS_PLACE
        rteActionsOnlyWP = RteActionsOnlyWP(track).getRteActionsOnlyWP()

        if (DEBUG_MODE) {
            debugMessages.add("KRUSKAL CLUSTERIZE waypoints rteActionsOnlyWP")
            rteActionsOnlyWP.forEach { debugMessages.add(it.parameterRteAction.toString()) }
        }

        // EDGES
        for (index in 0 until rteActionsOnlyWP.lastIndex) {
            if (DEBUG_MODE && (actionsToRteIndices[rteActionsOnlyWP[index]] == null ||
                            actionsToRteIndices[rteActionsOnlyWP[index + 1]] == null)){
                Log.e(tag, "unexpected null")
                exitProcess(-1)
            }

            edges.add(Edge(index, index + 1,
                    getDistBtwWpts(actionsToRteIndices[rteActionsOnlyWP[index]] ?: 0,
                            actionsToRteIndices[rteActionsOnlyWP[index + 1]] ?: 0)))
        }
        edges.sort()

        if (DEBUG_MODE) {
            debugMessages.add("KRUSKAL CLUSTERIZE edges sorted")
            edges.forEach { debugMessages.add(it.toString()) }
        }

        // FOREST
        for (index in rteActionsOnlyWP.indices) {
            forest.add(mutableListOf<Int>(index))
        }

        // DEBUG ASSERTS & PRINTS
        if (DEBUG_MODE) {
            val sizes = rteActionsOnlyWP.size == forest.size &&
                    if (rteActionsOnlyWP.isNotEmpty()) edges.size == rteActionsOnlyWP.size - 1 else true
            if (!sizes) {
                Log.e(tag,"sizes")
                exitProcess(-1)
            }
        }
    }

    data class Edge(val leftInd: Int, val rightInd: Int, val dist: Double)
        : Comparable<Edge> {
        override fun compareTo(other: Edge): Int {
            return dist.compareTo(other.dist)
        }
    }
}