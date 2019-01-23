package radim.outfit.core.export.work

import android.graphics.Color
import android.util.Log
import kotlin.math.roundToInt

const val SPEED_MIN_UNIT_AGNOSTIC = 1
const val SPEED_MAX_UNIT_AGNOSTIC = 130
const val SPEED_DEFAULT_M_S = 3.0F
const val MAX_HOURS_PICKER = 300

const val WARNING_COLOR = Color.RED

fun Float.kmhToMS() = this / 3.6F
fun Float.mphToMS() = this / 2.237F
fun speedMperSToKmh(ms: Float): Int = (ms * 3.6F).roundToInt()
fun speedMperSToMph(ms: Float): Int = (ms * 2.237F).roundToInt()
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

// the only accessor, has to clamp
fun getTrackTimesPOJO(speedMperS: Float, trackLength: Float): TrackTimesInPickerPOJO {
    val minutesTotal = ((trackLength / speedMperS) / 60F).roundToInt()
    val hours = minutesTotal / 60
    val minutes = minutesTotal % 60
    return if (hours in 0..MAX_HOURS_PICKER && minutes in 0..59)
        TrackTimesInPickerPOJO.TimeWithinBounds(hours, minutes)
    else TrackTimesInPickerPOJO.TimeOutOfBounds(
            clampTimeInTimePicker(hours, minutes).hours,
            clampTimeInTimePicker(hours, minutes).minutes,
            WARNING_COLOR)
}

// the only accessor, has to clamp
fun getTrackSpeedPOJO(kmhButt: Boolean?, mphButt: Boolean?, mPerS: Float): SpeedInPickerPOJO {
    val speedUnitAgnostic = when {
        (kmhButt != null && kmhButt) -> {
            speedMperSToKmh(mPerS)
        }
        (mphButt != null && mphButt) -> {
            speedMperSToMph(mPerS)
        }
        else -> speedMperSToKmh(mPerS)
    }
    return when {
        (speedUnitAgnostic in SPEED_MIN_UNIT_AGNOSTIC..SPEED_MAX_UNIT_AGNOSTIC) ->
            SpeedInPickerPOJO.SpeedWithinBounds(kmhButt, mphButt, speedUnitAgnostic)
        else -> SpeedInPickerPOJO.SpeedOutOfBounds(kmhButt, mphButt,
                clampSpeed(speedUnitAgnostic), WARNING_COLOR)
    }
}

// ALWAYS CLAMPED BOTH
sealed class TrackTimesInPickerPOJO {
    data class TimeWithinBounds(val hours: Int, val minutes: Int) : TrackTimesInPickerPOJO()
    data class TimeOutOfBounds(val hours: Int, val minutes: Int, val backgroundColor: Int)
        : TrackTimesInPickerPOJO()
}

// ALWAYS CLAMPED BOTH
sealed class SpeedInPickerPOJO {
    data class SpeedWithinBounds
    (val kmh: Boolean?, val mph: Boolean?, val speedUnitAgnostic: Int) : SpeedInPickerPOJO()

    data class SpeedOutOfBounds
    (val kmh: Boolean?, val mph: Boolean?, val speedUnitAgnostic: Int, val backgroundColor: Int)
        : SpeedInPickerPOJO()
}

data class SimpleTimePOJO(val hours: Int, val minutes: Int)
