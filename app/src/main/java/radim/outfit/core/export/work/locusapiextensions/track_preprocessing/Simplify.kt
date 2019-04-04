package radim.outfit.core.export.work.locusapiextensions.track_preprocessing

import radim.outfit.core.export.work.locusapiextensions.dismissibleRteActions

class Simplify (val debugMessages: MutableList<String>) {

    fun simplify (trackContainer: TrackContainer): TrackContainer {

        val survivors = trackContainer.track.waypoints.filter{
            !dismissibleRteActions.contains(it.parameterRteAction)
        }
        trackContainer.track.waypoints.clear()
        trackContainer.track.waypoints.addAll(survivors)

        return trackContainer
    }

}