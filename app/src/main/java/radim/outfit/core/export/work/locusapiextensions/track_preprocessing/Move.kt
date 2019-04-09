package radim.outfit.core.export.work.locusapiextensions.track_preprocessing

class Move(val debugMessages: MutableList<String>){

    fun move(howMuch: Double, trackContainer: TrackContainer): TrackContainer{
        // NOT PointRteAction.UNDEFINED; NOT PointRteAction.PASS_PLACE
        val rteActionsOnlyWP = RteActionsOnlyWP(trackContainer.track).getRteActionsOnlyWP()
















        return trackContainer
    }

}