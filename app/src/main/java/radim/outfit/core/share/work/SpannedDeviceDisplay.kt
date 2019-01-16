package radim.outfit.core.share.work

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import com.garmin.android.connectiq.IQDevice

class SpannedDeviceDisplay{

    private var devicesIdsOrder = mutableListOf<Long>()
    private val devicesIdsToSpannable = mutableMapOf<Long, SpannableString>()

    private val statusToColor: Map<IQDevice.IQDeviceStatus, Int> = mapOf(
            IQDevice.IQDeviceStatus.CONNECTED to Color.GREEN,
            IQDevice.IQDeviceStatus.NOT_CONNECTED to Color.RED,
            IQDevice.IQDeviceStatus.NOT_PAIRED to Color.BLUE,
            IQDevice.IQDeviceStatus.UNKNOWN to Color.BLACK
    )

    fun onDeviceEvent(device: IQDevice, status: IQDevice.IQDeviceStatus){
        devicesIdsToSpannable[device.deviceIdentifier] = getSpannableString(device, status)
        devicesIdsOrder.remove(device.deviceIdentifier)
        devicesIdsOrder = (mutableListOf(device.deviceIdentifier) + devicesIdsOrder).toMutableList()
    }

    private fun getSpannableString(device: IQDevice, status: IQDevice.IQDeviceStatus): SpannableString{
        val friendlyNameLength = device.friendlyName.length
        val rawString = "${device.friendlyName}: $status"
        val spannable = SpannableString(rawString)
        spannable.setSpan(
                ForegroundColorSpan(statusToColor[status]?: Color.BLACK),
                friendlyNameLength + 2, // start, plus ": "
                rawString.length, // end
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    fun getDisplay(): SpannableStringBuilder{
        val builder = SpannableStringBuilder()
        devicesIdsOrder.forEach {
            with (builder) {
                append(devicesIdsToSpannable[it])
                append(SpannableString("\n"))
            }
        }
        return builder
    }
}