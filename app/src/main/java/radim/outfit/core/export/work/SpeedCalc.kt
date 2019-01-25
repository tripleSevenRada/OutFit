package radim.outfit.core.export.work

import android.util.Log
import kotlin.math.roundToInt

const val SPEED_MIN_UNIT_AGNOSTIC = 1
const val SPEED_MAX_UNIT_AGNOSTIC = 150
const val SPEED_DEFAULT_M_S = 3.0F
const val MAX_HOURS_PICKER = 71

fun Float.kmhToMs(): Float = this / 3.6F
fun Int.kmhToMs(): Float = this.toFloat().kmhToMs()
fun Float.mphToMs(): Float = this / 2.237F
fun Int.mphToMs(): Float = this.toFloat().mphToMs()
fun Float.msToKmh(): Int = (this * 3.6F).roundToInt()
fun Float.msToMph(): Int = (this * 2.237F).roundToInt()
fun TrackTimesInPickerPOJO.toSeconds() = (hours * 60 * 60) + (minutes * 60)

// CLAMPS BEGIN
fun clampSpeedForSpeedPicker(speed: Int) = when {
    (speed > SPEED_MAX_UNIT_AGNOSTIC) -> run{
        SPEED_MAX_UNIT_AGNOSTIC
    }
    (speed < SPEED_MIN_UNIT_AGNOSTIC) -> run{
        SPEED_MIN_UNIT_AGNOSTIC
    }
    else -> run{
        speed
    }
}

fun clampTimeForTimePicker(data: TrackTimesInPickerPOJO): TrackTimesInPickerPOJO {
    if (data.minutes !in 0..59) Log.e("clampTime", "minutes !in 0..59")
    if (data.hours !in 0..MAX_HOURS_PICKER) Log.e("clampTime", "hours !in 0..MAX_HOURS_PICKER")
    return when {
        (data.minutes !in 0..59) -> TrackTimesInPickerPOJO(0,0)
        (data.hours < 0 || data.minutes < 0) -> TrackTimesInPickerPOJO(0, 0)
        (data.hours > MAX_HOURS_PICKER) -> TrackTimesInPickerPOJO(MAX_HOURS_PICKER, 59)
        else -> TrackTimesInPickerPOJO(data.hours, data.minutes)
    }
}
// CLAMPS END

fun getTrackTimesPOJO(speedMperS: Float, trackLength: Float): TrackTimesInPickerPOJO {
    val minutesTotal = ((trackLength / speedMperS) / 60F).roundToInt()
    val hours = minutesTotal / 60
    val minutes = minutesTotal % 60
    return TrackTimesInPickerPOJO(hours, minutes)
}

fun getTrackSpeed(mPerS: Float, convertInUnits: Float.() -> Int): Int {
    val speedUnitAgnostic = mPerS.convertInUnits()
    return clampSpeedForSpeedPicker(speedUnitAgnostic)
}

data class TrackTimesInPickerPOJO(val hours: Int, val minutes: Int)

