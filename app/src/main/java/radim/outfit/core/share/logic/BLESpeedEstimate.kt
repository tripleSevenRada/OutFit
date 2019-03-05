package radim.outfit.core.share.logic

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import radim.outfit.core.export.work.ColorCrossFader
import radim.outfit.core.export.work.Span
import radim.outfit.getString

const val START_OF_DOWNLOAD_TIME_WARNING = 15 // sec
const val NO_MORE_DOWNLOAD_TIME_WARNING = 150 // sec

fun getEstimatedDownloadTimeInSeconds(sizeB: Long): Int {
    // https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5751532/
    // https://drive.google.com/open?id=1eAhK6x2wASKAdt9u3CFujjW7rDF7qFbG
    return (sizeB / 500).toInt()
}

fun getSpannableDownloadInfo(secondsTot: Int, ctx: AppCompatActivity): SpannableString {

    //TODO
    val colorFrom = Color.parseColor("#e3eaa7")// toLow in colors
    val colorTo = Color.parseColor("#eca1a6")// toHigh in colors

    val colorCrossFadeSpan = Span(START_OF_DOWNLOAD_TIME_WARNING.toDouble(), NO_MORE_DOWNLOAD_TIME_WARNING.toDouble())
    val colorCrossFader = ColorCrossFader(colorFrom, colorTo, colorCrossFadeSpan.getDelta())

    val color: Int = when {
        (colorCrossFadeSpan.isInFrom(secondsTot.toDouble())) -> colorFrom
        (colorCrossFadeSpan.isInTo(secondsTot.toDouble())) -> colorTo
        else -> colorCrossFader.colorCrossFade(
                colorCrossFadeSpan.getInSpanRelativeToTo(secondsTot.toDouble()))
    }

    val minsS = ctx.getString("minutes")
    val secsS = ctx.getString("seconds")
    val mins = secondsTot / 60
    val secs = secondsTot % 60
    val prefix = ctx.getString("est_download_time")
    val timeS = if (mins > 0) "$mins $minsS, $secs $secsS" else "$secs $secsS"
    val publish = "$prefix\n\t\t\t$timeS"
    val spannable = SpannableString(publish)
    spannable.setSpan(BackgroundColorSpan(color), (publish.length - timeS.length), publish.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return spannable
}
