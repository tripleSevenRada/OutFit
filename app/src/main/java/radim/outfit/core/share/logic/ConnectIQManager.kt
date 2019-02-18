package radim.outfit.core.share.logic

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.ConnectIQ.IQApplicationInfoListener
import radim.outfit.*
import radim.outfit.core.viewmodels.ViewResultsActivityViewModel

class ConnectIQManager(
        private val ctx: AppCompatActivity,
        private val onStartInit: () -> Unit,
        private val onFinishInit: () -> Unit,
        private val onDeviceEvent: (IQDevice, IQDevice.IQDeviceStatus) -> Unit,
        private val onAppEvent: (IQDevice, IQApp.IQAppStatus) -> Unit,
        private val onFirstINFITDetected: (String) -> Unit
) {

    private var firstINFITReported = false
    private val tag = "ConnIQList"
    private val connectionType = ConnectIQ.IQConnectType.WIRELESS
    private val connectIQ: ConnectIQ = ConnectIQ.getInstance(ctx, connectionType)
    private val connectIQListener: ConnectIQ.ConnectIQListener = ConnectIQLifecycleListener()
    private val companionAppId = "77481aa88425463bb49961ecf99332d3"
    private val companionAppRequiredVersion = 100

    private var connectIQIsInitialized = false
    private var connectIQIsBeingInitialized = false

    fun startConnectIQ() {
        if (!connectIQIsInitialized &&
                !connectIQIsBeingInitialized) {
            connectIQIsBeingInitialized = true
            onStartInit()
            Log.i(tag, "init")
            connectIQ.initialize(ctx, true, connectIQListener)
        }
    }

    fun shutDownConnectIQ() {
        decoratedDevices.clear()
        unregisterForDeviceEvents()
        shutdown()
    }

    private fun unregisterForDeviceEvents() {
        if (connectIQIsInitialized) {
            connectIQ.connectedDevices?.forEach {
                if (it != null) connectIQ.unregisterForDeviceEvents(it)
            }
        }
    }

    fun goToTheStore() = connectIQ.openStore(companionAppId)

    fun showHowToInFitDialog(message: String) {
        val dialog = getBundledDialog(
                message,
                "",
                ctx.getString("infit_dialog_never_ask_again"),
                ctx.getString("ok")
        )
        // Verify that the host activity implements the callback interface
        val mListener: IQAppIsInvalidDialogFragment.IQAppIsInvalidDialogListener
        try {
            // Instantiate the IQAppIsInvalidDialogListener so we can send events to the host
            mListener = ctx as IQAppIsInvalidDialogFragment.IQAppIsInvalidDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException((ctx.toString() +
                    " must implement IQAppIsInvalidDialogListener"))
        }
        mListener.setDialogVisible(true)
        mListener.setDialogType(DialogType.HowToInFitInfo("say keep activity in foreground"))
        showDialog(dialog)
    }

    private fun shutdown() {
        if (connectIQIsInitialized) connectIQ.shutdown(ctx)
        connectIQIsInitialized = false
    }

    private val decoratedDevices = mutableSetOf<Long>()

    // connectIQListener
    inner class ConnectIQLifecycleListener : ConnectIQ.ConnectIQListener {
        // Called when initialization fails.
        override fun onInitializeError(p0: ConnectIQ.IQSdkErrorStatus?) {
            // A failure has occurred during initialization. Inspect
            // the IQSdkErrorStatus value for more information regarding the failure.
            val errorMessage = p0?.toString() ?: "ConnectIQ platform init error"
            Log.e(tag, "onInitializeError: $errorMessage")
            FailGracefullyLauncher().failGracefully(ctx, errorMessage)
        }

        // Called when the SDK has been successfully initialized
        override fun onSdkReady() {
            connectIQIsInitialized = true
            connectIQIsBeingInitialized = false
            // Do any post initialization setup.
            Log.i(tag, "onSdkReady")
            // list connected and known devices
            val connectedDevices = connectIQ.connectedDevices
            val knownDevices = connectIQ.knownDevices
            if (DEBUG_MODE) {
                knownDevices.forEach {
                    Log.i(tag, "Known!DEVICE: deviceIdentifier: ${it?.deviceIdentifier}")
                    Log.i(tag, "Known!DEVICE: friendlyName: ${it?.friendlyName}")
                    Log.i(tag, "Known!DEVICE: status: ${it?.status}")
                }
            }
            if (connectedDevices != null && connectedDevices.size > 0) {
                connectedDevices.forEach {
                    if (DEBUG_MODE) {
                        Log.i(tag, "CONNECTED!DEVICE: deviceIdentifier: ${it?.deviceIdentifier}")
                        Log.i(tag, "CONNECTED!DEVICE: friendlyName: ${it?.friendlyName}")
                        Log.i(tag, "CONNECTED!DEVICE: status: ${it?.status}")
                        Log.i(tag, "CONNECTED!DEVICE: describeContents(): ${it?.describeContents()}")
                    }
                    if (it != null) {
                        onDeviceEvent(it, it.status)
                        decorateDevice(it)
                    }
                }
            }
            onFinishInit()
        }

        // Called when the SDK has been shut down
        override fun onSdkShutDown() {
            // Take care of any post shutdown requirements
            Log.i(tag, "onSdkShutDown")
        }
    }

    private fun decorateDevice(device: IQDevice) {
        if (decoratedDevices.contains(device.deviceIdentifier)) {
            if (DEBUG_MODE) Log.e(tag, "SHORT CIRCUITING decorating device: ${device.friendlyName}")
            return
        } else {
            if (DEBUG_MODE) Log.w(tag, "decorating device: ${device.friendlyName}")
            // keep track of what I have seen
            decoratedDevices.add(device.deviceIdentifier)
            // Register to receive status updates
            connectIQ.registerForDeviceEvents(device, DeviceEventListener())
            //
            setCompanionAppListeners(device)
        }
    }

    inner class DeviceEventListener : ConnectIQ.IQDeviceEventListener {
        override fun onDeviceStatusChanged(device: IQDevice?, status: IQDevice.IQDeviceStatus?) {
            if (DEBUG_MODE) {
                Log.i(tag, "STATUS_CHANGED, deviceIdentifier: ${device?.deviceIdentifier}")
                Log.i(tag, "STATUS_CHANGED, friendlyName: ${device?.friendlyName}")
                Log.i(tag, "STATUS_CHANGED, newStatus: ${status?.toString()}")
            }
            if (device != null && status != null) {
                if (!decoratedDevices.contains(device.deviceIdentifier)) {
                    onDeviceEvent(device, status)
                    decorateDevice(device)
                    decoratedDevices.add(device.deviceIdentifier)
                } else {
                    onDeviceEvent(device, status)
                }
            }
        }
    }

    // APP
    private fun setCompanionAppListeners(device: IQDevice) {
        connectIQ.getApplicationInfo(companionAppId,
                device,
                object : IQApplicationInfoListener {

                    override fun onApplicationInfoReceived(app: IQApp?) {
                        if (app != null) {
                            if (app.status != null) {
                                if (app.version() < companionAppRequiredVersion) {
                                    // Prompt the user to upgrade
                                    // TODO
                                    if (DEBUG_MODE) {
                                        Log.w(tag, "my current required version is: $companionAppRequiredVersion")
                                        Log.w(tag, "detected app version on $device: ${app.version()}")
                                    }
                                }
                                if (DEBUG_MODE) {
                                    Log.w(tag, "app.status = ${app.status}")
                                    Log.w(tag, "deviceIdentifier: ${device.deviceIdentifier}")
                                    Log.w(tag, "device friendlyName: ${device.friendlyName}")
                                    Log.w(tag, "device status: ${device.status}")
                                }
                                onAppEvent(device, app.status)
                                if (app.status == IQApp.IQAppStatus.INSTALLED) {
                                    if (!firstINFITReported) onFirstINFITDetected(getFriendlyName(device))
                                    firstINFITReported = true
                                }
                            }
                        }
                    }

                    override fun onApplicationNotInstalled(applicationId: String) {
                        // Prompt user with information only if not disabled by Never ask again
                        val prefs = ctx.getSharedPreferences(
                                ctx.getString(R.string.main_activity_preferences), Context.MODE_PRIVATE)
                        if (prefs.contains("dialog_app_not_installed_disabled") &&
                                prefs.getBoolean("dialog_app_not_installed_disabled", false)) {
                            onAppEvent(device, IQApp.IQAppStatus.NOT_INSTALLED)
                            return
                        }
                        // Verify that the host activity implements the callback interface
                        val mListener: IQAppIsInvalidDialogFragment.IQAppIsInvalidDialogListener
                        try {
                            // Instantiate the IQAppIsInvalidDialogListener so we can send events to the host
                            mListener = ctx as IQAppIsInvalidDialogFragment.IQAppIsInvalidDialogListener
                        } catch (e: ClassCastException) {
                            // The activity doesn't implement the interface, throw exception
                            throw ClassCastException((ctx.toString() +
                                    " must implement IQAppIsInvalidDialogListener"))
                        }
                        val installDialogShown = mListener.getDialogVisible() &&
                                mListener.getDialogType() is DialogType.NotInstalled
                        if (!installDialogShown) {
                            val dialog = getBundledDialog(
                                    "${ctx.getString("infit_dialog_infit_not_installed_message")} ${getFriendlyName(device)}",
                                    ctx.getString("infit_dialog_take_me_to_the_store"),
                                    ctx.getString("infit_dialog_never_ask_again"),
                                    ctx.getString("later")
                            )
                            mListener.setDialogVisible(true)
                            mListener.setDialogType(DialogType.NotInstalled("say not installed"))
                            showDialog(dialog)
                        }
                        if (DEBUG_MODE) Log.w(tag, "app not installed on ${device.friendlyName}")
                        onAppEvent(device, IQApp.IQAppStatus.NOT_INSTALLED)
                    }

                    fun getFriendlyName(deviceToQuery: IQDevice): String {
                        return if (deviceToQuery.friendlyName.isEmpty()) {
                            val viewModel = ViewModelProviders.of(ctx).get(ViewResultsActivityViewModel::class.java)
                            val friendlyNameStored = viewModel.idToFriendlyName[deviceToQuery.deviceIdentifier]
                            friendlyNameStored ?: "device"
                        } else deviceToQuery.friendlyName
                    }
                })
    }

    private fun getBundledDialog(
            message: String,
            positive: String,
            negative: String,
            neutral: String): IQAppIsInvalidDialogFragment {
        val dialog = IQAppIsInvalidDialogFragment()
        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0)
        val bundle = Bundle()
        bundle.putString("message", message)
        bundle.putString("positive", positive)
        bundle.putString("negative", negative)
        bundle.putString("neutral", neutral)
        dialog.arguments = bundle
        return dialog
    }

    private fun showDialog(dialog: IQAppIsInvalidDialogFragment) {
        val manager = ctx.supportFragmentManager
        if (manager != null) {
            dialog.show(manager, "showDialog")
        } else {
            Log.e(tag, "manager == null")
        }
    }
}