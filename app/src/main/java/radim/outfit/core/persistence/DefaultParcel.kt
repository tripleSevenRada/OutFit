package radim.outfit.core.persistence

import android.support.v7.app.AppCompatActivity
import android.text.SpannableString
import radim.outfit.ViewResultsParcel

fun getDefaultParcel(ctx: AppCompatActivity): ViewResultsParcel{
    val title = "default title"
    val messages = listOf<SpannableString>(
            SpannableString("default message1"),
            SpannableString("default message2"))
    val buffer = arrayOf("")
    val filenameToCoursename = mapOf<String, String>(Pair("",""))
    return ViewResultsParcel(title, messages, buffer, filenameToCoursename, ViewResultsParcel.Type.DEFAULT)
}