package radim.outfit.core.export.work.locusapiextensions.track_preprocessing

import locus.api.objects.extra.Location

class Clustering{


}

data class Cluster (val centroid: Location){

    val members = mutableSetOf<Location>()

}