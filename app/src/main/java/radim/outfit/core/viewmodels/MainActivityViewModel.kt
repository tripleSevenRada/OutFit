package radim.outfit.core.viewmodels

import android.arch.lifecycle.ViewModel
import locus.api.objects.extra.Track

class MainActivityViewModel: ViewModel(){

    var exportInProgress = false
    var track: Track? = null

    //override fun onCleared() {
        //super.onCleared()
    //}

}