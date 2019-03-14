package radim.outfit.core.viewmodels

import android.arch.lifecycle.ViewModel
import radim.outfit.core.export.work.locusapiextensions.track_preprocessing.TrackContainer

class MainActivityViewModel: ViewModel(){

    var exportInProgress = false
    var trackContainer: TrackContainer? = null
    var preprocessInProgress = false
    var speedPickerFragmentShown = false

    //override fun onCleared() {
        //super.onCleared()
    //}

}