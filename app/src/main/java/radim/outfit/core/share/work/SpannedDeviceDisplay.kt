package radim.outfit.core.share.work

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import radim.outfit.getString

class SpannedDeviceDisplay {

    private var devicesIdsOrder = mutableListOf<Long>()
    private val devicesIdsToSpannable = mutableMapOf<Long, SpannableString>()
    private val tag = "spannDisplay"

    private val deviceStatusToColor: Map<IQDevice.IQDeviceStatus, Int> = mapOf(
            IQDevice.IQDeviceStatus.CONNECTED to Color.GREEN,
            IQDevice.IQDeviceStatus.NOT_CONNECTED to Color.RED,
            IQDevice.IQDeviceStatus.NOT_PAIRED to Color.BLACK,
            IQDevice.IQDeviceStatus.UNKNOWN to Color.BLACK
    )
    private val appStatusToColor: Map<IQApp.IQAppStatus, Int> = mapOf(
            IQApp.IQAppStatus.INSTALLED to Color.GREEN,
            IQApp.IQAppStatus.NOT_INSTALLED to Color.RED,
            IQApp.IQAppStatus.NOT_SUPPORTED to Color.RED,
            IQApp.IQAppStatus.UNKNOWN to Color.BLACK
    )

    fun onDeviceEvent(device: IQDevice, status: IQDevice.IQDeviceStatus) {
        devicesIdsToSpannable[device.deviceIdentifier] = getSpannableString(device, status)
        devicesIdsOrder.remove(device.deviceIdentifier)
        devicesIdsOrder = (mutableListOf(device.deviceIdentifier) + devicesIdsOrder).toMutableList()
    }

    fun onAppEvent(device: IQDevice, appStatus: IQApp.IQAppStatus, ctx: AppCompatActivity) {
        if (!devicesIdsToSpannable.keys.contains(device.deviceIdentifier)) {
            Log.e(tag, "onAppEvent: I have not seen this device yet: ${device.friendlyName}")
            return
        } else {
            val preexistingSpannable: SpannableString? = devicesIdsToSpannable[device.deviceIdentifier]
            if (preexistingSpannable != null) {
                val infit = ctx.getString("infit")
                val infitSpannable = SpannableString(", $infit: ")
                infitSpannable.setSpan(
                        ForegroundColorSpan(Color.BLACK),
                        0,
                        infitSpannable.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                val appStatusSpannable = SpannableString(appStatus.toString())
                appStatusSpannable.setSpan(
                        ForegroundColorSpan(appStatusToColor[appStatus] ?: Color.BLACK),
                        0,
                        appStatusSpannable.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                val builder = SpannableStringBuilder()
                with(builder) {
                    append(preexistingSpannable)
                    append(infitSpannable)
                    append(appStatusSpannable)
                }
                devicesIdsToSpannable[device.deviceIdentifier] = SpannableString.valueOf(builder)
            } else return
        }
    }

    private fun getSpannableString(device: IQDevice, status: IQDevice.IQDeviceStatus): SpannableString {
        val friendlyNameLength = device.friendlyName.length
        val rawString = "${device.friendlyName}: $status"
        val spannable = SpannableString(rawString)
        spannable.setSpan(
                ForegroundColorSpan(deviceStatusToColor[status] ?: Color.BLACK),
                friendlyNameLength + 2, // start, plus ": "
                rawString.length, // end
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    fun getDisplay(): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        devicesIdsOrder.forEach {
            with(builder) {
                append(devicesIdsToSpannable[it])
                append(SpannableString("\n"))
            }
        }
        return builder
    }
}