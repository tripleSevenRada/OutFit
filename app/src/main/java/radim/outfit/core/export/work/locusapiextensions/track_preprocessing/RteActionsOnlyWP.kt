package radim.outfit.core.export.work.locusapiextensions.track_preprocessing
import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Point
import locus.api.objects.extra.Track

class RteActionsOnlyWP(val track: Track) {
    fun getRteActionsOnlyWP(): List<Point> {
        return track.waypoints.filter {
            it != null &&
                    it.parameterRteAction != PointRteAction.UNDEFINED &&
                    it.parameterRteAction != PointRteAction.PASS_PLACE
        }
    }
}