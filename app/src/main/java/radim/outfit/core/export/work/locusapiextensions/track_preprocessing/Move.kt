package radim.outfit.core.export.work.locusapiextensions.track_preprocessing

import android.util.Log
import radim.outfit.core.export.work.locusapiextensions.stringdumps.PointStringDump

class Move(val debugMessages: MutableList<String>){

    fun move(howMuch: Double, trackContainer: TrackContainer): TrackContainer{

        val tag = "MOVE"

        // trackContainer.definedRteActionsToShiftedIndices //contains all except UNDEFINED

        // NOT PointRteAction.UNDEFINED; NOT PointRteAction.PASS_PLACE
        val rteActionsOnlyWP = RteActionsOnlyWP(trackContainer.track).getRteActionsOnlyWP()

















        return trackContainer
    }

}