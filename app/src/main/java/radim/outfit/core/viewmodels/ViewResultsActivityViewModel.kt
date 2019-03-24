package radim.outfit.core.viewmodels

import android.arch.lifecycle.ViewModel
import android.text.SpannableStringBuilder
import radim.outfit.DialogType
import radim.outfit.ViewResultsParcel
import radim.outfit.core.persistence.ParcelDatabase
import java.io.File

class ViewResultsActivityViewModel: ViewModel(){
    var fileOpsDone = false
    var parcelPersistenceDone = false
    var bufferHead: File? = null

    var idToFriendlyName = mutableMapOf<Long, String>()
    var dialogType: DialogType? = null
    var dialogShown = false
    var dialogHowToInfitIncarnation = false
    var statsSpannableStringBuilder: SpannableStringBuilder? = null
}