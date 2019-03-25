package radim.outfit.core.persistence

import android.support.v7.app.AppCompatActivity
import android.text.SpannableString
import radim.outfit.ViewResultsParcel
import radim.outfit.getString

fun getDefaultParcel(ctx: AppCompatActivity): ViewResultsParcel{
    val title = "default title"
    val messages = listOf<SpannableString>(
            SpannableString(ctx.getString("default_parcel1")),
            SpannableString(ctx.getString("default_parcel2")),
            SpannableString(ctx.getString("default_parcel3.1")),
            SpannableString(ctx.getString("default_parcel3.2")),
            SpannableString(ctx.getString("default_parcel3.3")),
            SpannableString(ctx.getString("default_parcel4"))
    )
    val buffer = arrayOf("")
    val filenameToCoursename = mapOf<String, String>(Pair("",""))
    return ViewResultsParcel(title, messages, buffer, filenameToCoursename, ViewResultsParcel.Type.DEFAULT)
}