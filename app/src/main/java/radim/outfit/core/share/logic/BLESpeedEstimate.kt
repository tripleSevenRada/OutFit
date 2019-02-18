package radim.outfit.core.share.logic

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import radim.outfit.getString

fun getEstimatedDownloadTimeInSeconds(sizeB: Long): Int{
    // https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5751532/
    // https://drive.google.com/open?id=1eAhK6x2wASKAdt9u3CFujjW7rDF7qFbG
    return (sizeB / 500).toInt()
}
fun getSpannableDownloadInfo(secondsTot: Int, ctx: AppCompatActivity, size: Long): SpannableString{
    val color = when (secondsTot) {
        in 0..20 -> Color.GREEN
        in 21..60 -> Color.rgb(255,140,0)
        else -> Color.RED
    }
    val minsS = ctx.getString("minutes")
    val secsS = ctx.getString("seconds")
    val mins = secondsTot / 60
    val secs = secondsTot % 60
    val prefix = ctx.getString("est_download_time")
    val timeS = if(mins > 0) "$mins $minsS, $secs $secsS" else "$secs $secsS"
    val publish = "$prefix\n\t\t\t$timeS"
    val spannable = SpannableString(publish)
    spannable.setSpan(ForegroundColorSpan(color), (publish.length - timeS.length), publish.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return spannable
}
