package radim.outfit.core.viewmodels

import android.arch.lifecycle.ViewModel
import radim.outfit.core.export.work.locusapiextensions.TrackContainer

class MainActivityViewModel: ViewModel(){

    var exportInProgress = false
    var trackContainer: TrackContainer? = null

    //override fun onCleared() {
        //super.onCleared()
    //}

}