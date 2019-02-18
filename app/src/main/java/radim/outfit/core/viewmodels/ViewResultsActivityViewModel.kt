package radim.outfit.core.viewmodels

import android.arch.lifecycle.ViewModel
import android.text.SpannableStringBuilder
import radim.outfit.DialogType
import java.io.File

class ViewResultsActivityViewModel: ViewModel(){

    var fileOperationsDone = false
    var bufferHead: File? = null
    var idToFriendlyName = mutableMapOf<Long, String>()
    var dialogType: DialogType? = null
    var dialogShown = false
    var statsSpannableStringBuilder: SpannableStringBuilder? = null
}