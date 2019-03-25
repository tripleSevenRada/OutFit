package radim.outfit.core.async

import android.text.SpannableString
import radim.outfit.ViewResultsParcel

fun ViewResultsParcel.getShallow(): ShallowParcel{
    return ShallowParcel(
            this.messages.toList(),
            this.buffer.toList(),
            this.fileNameToCourseName.toMap(),
            this.type
    )
}

data class ShallowParcel(
        val messages: List<SpannableString>,
        val buffer: List<String>,
        val fileNameToCoursename: Map<String, String>,
        val type: ViewResultsParcel.Type)
