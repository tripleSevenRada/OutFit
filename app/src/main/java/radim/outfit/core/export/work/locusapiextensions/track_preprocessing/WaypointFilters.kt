package radim.outfit.core.export.work.locusapiextensions.track_preprocessing

import locus.api.objects.enums.PointRteAction
import radim.outfit.core.export.work.locusapiextensions.alwaysDismissibleRteActions
import radim.outfit.core.export.work.locusapiextensions.dismissibleRteActions

open class WaypointFilter(
        private val trackContainer: TrackContainer,
        private val filterOut: Set<PointRteAction>) {
    fun filter(): TrackContainer {
        val survivors = trackContainer.track.waypoints.filter {
            !filterOut.contains(it.parameterRteAction)
        }
        trackContainer.track.waypoints.clear()
        trackContainer.track.waypoints.addAll(survivors)
        return trackContainer
    }
}

class Simplify(container: TrackContainer) : WaypointFilter(
        container,
        dismissibleRteActions
) {
    fun simplify(): TrackContainer {
        return super.filter()
    }
}

class FilterAlwaysDismissible(container: TrackContainer) : WaypointFilter(
        container,
        alwaysDismissibleRteActions
) {
    fun filterAlwaysDismissible(): TrackContainer {
        return super.filter()
    }
}
