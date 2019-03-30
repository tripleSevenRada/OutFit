package radim.outfit.core.export.work.locusapiextensions.track_preprocessing

import android.util.Log
import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Track
import locus.api.objects.utils.LocationCompute.computeDistanceFast

class Kruskal{

    private val tag = "Kruskal"
    private val edges = mutableListOf<Edge>()
    private val forest = mutableListOf<Set<Int>>()

    fun clusterize(minDistWP: Double, track: Track): Track{
        preprocess(track)

        return track
    }

    private fun preprocess(track: Track){

        fun getDistBtwWpts(rteIndLeft: Int, rteIndRight: Int): Double{
            var accumulator = 0.0
            for (i in rteIndLeft until rteIndRight){
                accumulator += computeDistanceFast(track.getPoint(i), track.getPoint(i + 1))
            }
            return accumulator
        }

        val rteActionsOnlyWP = track.waypoints.filter {
            it.paramRteIndex != -1 &&
                    it.parameterRteAction != PointRteAction.UNDEFINED &&
                    it.parameterRteAction != PointRteAction.PASS_PLACE
        }

        for (index in 0 until rteActionsOnlyWP.lastIndex) {
            edges.add(Edge(index, index + 1,
                    getDistBtwWpts(rteActionsOnlyWP[index].paramRteIndex,
                            rteActionsOnlyWP[index + 1].paramRteIndex)))
        }

        edges.sort()


    }

    data class Edge (val leftInd: Int, val rightInd: Int, val dist: Double)
        :Comparable<Edge>{
        override fun compareTo(other: Edge): Int {
            return dist.compareTo(other.dist)
        }
    }
}