package radim.outfit.core.persistence

import android.support.v7.app.AppCompatActivity
import android.text.SpannableString
import radim.outfit.ViewResultsParcel

fun getErrorParcel(ctx: AppCompatActivity): ViewResultsParcel {
    val title = "error title"
    val messages = listOf<SpannableString>(
            SpannableString("error message1"),
            SpannableString("error message2"))
    val buffer = arrayOf("")
    val filenameToCoursename = mapOf<String, String>(Pair("",""))
    return ViewResultsParcel(title, messages, buffer, filenameToCoursename, ViewResultsParcel.Type.DEFAULT)
}