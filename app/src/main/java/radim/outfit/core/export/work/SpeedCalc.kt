package radim.outfit.core.export.work

import kotlin.math.roundToInt

const val SPEED_MIN_UNIT_AGNOSTIC = 1
const val SPEED_MAX_UNIT_AGNOSTIC = 130
const val SPEED_DEFAULT_M_S = 3.0F
const val MAX_HOURS_PICKER = 47

fun Float.kmhToMS() = this / 3.6F
fun Float.mphToMS() = this / 2.237F
fun speedMperSToKmh(ms: Float): Int = (ms * 3.6F).toInt()
fun speedMperSToMph(ms: Float): Int = (ms * 2.237F).toInt()
fun clampSpeed(speed: Int) = if (speed > SPEED_MAX_UNIT_AGNOSTIC) SPEED_MAX_UNIT_AGNOSTIC
else if (speed < SPEED_MIN_UNIT_AGNOSTIC) SPEED_MIN_UNIT_AGNOSTIC
else speed

fun clampSpeedMS(speed: Float) = if (speed > SPEED_MAX_UNIT_AGNOSTIC.toFloat().mphToMS())
    SPEED_MAX_UNIT_AGNOSTIC.toFloat().mphToMS()
else if (speed < SPEED_MIN_UNIT_AGNOSTIC.toFloat().kmhToMS()) SPEED_MIN_UNIT_AGNOSTIC.toFloat().kmhToMS()
else speed

fun getTrackTimesPOJO(btnKmh: Boolean?, btnMph: Boolean?, units: Int, trackLength: Float): TrackTimesPOJO {
    val speedMperS = if (btnKmh != null && btnKmh) units.toFloat().kmhToMS()
    else if (btnMph != null && btnMph) units.toFloat().mphToMS()
    else units.toFloat().kmhToMS()
    val minutesTotal = ((trackLength / speedMperS) / 60F).roundToInt()
    return TrackTimesPOJO(minutesTotal / 60, minutesTotal % 60)
}

data class TrackTimesPOJO(val hours: Int, val minutes: Int) {
    //data class Mins (val hours: Int, val minutes: Int)
    //data class Maxs (val hours: Int, val minutes: Int)
}