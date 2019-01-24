package radim.outfit.core.export.work

import android.graphics.Color
import android.util.Log
import kotlin.math.roundToInt

const val SPEED_MIN_UNIT_AGNOSTIC = 1
const val SPEED_MAX_UNIT_AGNOSTIC = 50
const val SPEED_DEFAULT_M_S = 3.0F
const val MAX_HOURS_PICKER = 13

const val WARNING_COLOR = Color.RED

fun Float.kmhToMS(): Float = this / 3.6F
fun Int.kmhToMS(): Float = this.toFloat().kmhToMS()
fun Float.mphToMS(): Float = this / 2.237F
fun Int.mphToMS(): Float = this.toFloat().mphToMS()
fun Float.speedMperStoKmh(): Int = (this * 3.6F).roundToInt()
fun Float.speedMperStoMph(): Int = (this * 2.237F).roundToInt()
fun TrackTimesInPickerPOJO.WithinBounds.toSeconds() = (hours * 60 * 60) + (minutes * 60)
fun TrackTimesInPickerPOJO.OutOfBounds.toSeconds() = (hours * 60 * 60) + (minutes * 60)
fun clampSpeed(speed: Int) = when {
    (speed > SPEED_MAX_UNIT_AGNOSTIC) -> SPEED_MAX_UNIT_AGNOSTIC
    (speed < SPEED_MIN_UNIT_AGNOSTIC) -> SPEED_MIN_UNIT_AGNOSTIC
    else -> speed
}

fun clampSpeedMS(speed: Float) = when {
    (speed > SPEED_MAX_UNIT_AGNOSTIC.toFloat().mphToMS()) -> SPEED_MAX_UNIT_AGNOSTIC.toFloat().mphToMS()
    (speed < SPEED_MIN_UNIT_AGNOSTIC.toFloat().kmhToMS()) -> SPEED_MIN_UNIT_AGNOSTIC.toFloat().kmhToMS()
    else -> speed
}

fun clampTimeInTimePicker(hours: Int, minutes: Int): SimpleTimePOJO {
    if (minutes !in 0..59) Log.e("clampTimeInTimePicker", "minutes !in 0..59")
    return when {
        (hours < 0 || minutes < 0) -> SimpleTimePOJO(0, 0)
        (hours > MAX_HOURS_PICKER) -> SimpleTimePOJO(MAX_HOURS_PICKER, 59)
        else -> SimpleTimePOJO(hours, minutes)
    }
}

fun getTrackTimesPOJO(speedMperS: Float, trackLength: Float): TrackTimesInPickerPOJO {
    val minutesTotal = ((trackLength / speedMperS) / 60F).roundToInt()
    val hours = minutesTotal / 60
    val minutes = minutesTotal % 60
    return if (hours in 0..MAX_HOURS_PICKER && minutes in 0..59)
        TrackTimesInPickerPOJO.WithinBounds(hours, minutes)
    else TrackTimesInPickerPOJO.OutOfBounds(
            clampTimeInTimePicker(hours, minutes).hours,
            clampTimeInTimePicker(hours, minutes).minutes,
            WARNING_COLOR)
}

fun getTrackSpeedPOJO(mPerS: Float, convertInUnits: Float.() -> Int): SpeedInPickerPOJO {
    val speedUnitAgnostic = mPerS.convertInUnits()
    return when {
        (speedUnitAgnostic in SPEED_MIN_UNIT_AGNOSTIC..SPEED_MAX_UNIT_AGNOSTIC) ->
            SpeedInPickerPOJO.WithinBounds(speedUnitAgnostic)
        else -> SpeedInPickerPOJO.OutOfBounds(clampSpeed(speedUnitAgnostic), WARNING_COLOR)
    }
}

sealed class TrackTimesInPickerPOJO {
    data class WithinBounds(val hours: Int, val minutes: Int) : TrackTimesInPickerPOJO()
    data class OutOfBounds(val hours: Int, val minutes: Int, val backgroundColor: Int)
        : TrackTimesInPickerPOJO()
}

sealed class SpeedInPickerPOJO {
    data class WithinBounds(val speedUnitAgnostic: Int) : SpeedInPickerPOJO()
    data class OutOfBounds(val speedUnitAgnostic: Int, val backgroundColor: Int) : SpeedInPickerPOJO()
}

data class SimpleTimePOJO(val hours: Int, val minutes: Int)
