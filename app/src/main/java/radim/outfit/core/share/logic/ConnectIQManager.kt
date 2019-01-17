package radim.outfit.core.share.logic

import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import radim.outfit.DEBUG_MODE
import radim.outfit.FailGracefullyLauncher
import com.garmin.android.connectiq.ConnectIQ.IQApplicationInfoListener

class ConnectIQManager(
        private val ctx: AppCompatActivity,
        private val onStartInit: () -> Unit,
        private val onFinishInit: () -> Unit,
        private val onDeviceEvent: (IQDevice, IQDevice.IQDeviceStatus) -> Unit,
        private val onAppEvent: (IQDevice, IQApp.IQAppStatus) -> Unit
) {

    private val tag = "ConnIQList"
    private val connectionType = ConnectIQ.IQConnectType.TETHERED
    private val connectIQ: ConnectIQ = ConnectIQ.getInstance(ctx, connectionType)
    private val connectIQListener: ConnectIQ.ConnectIQListener = ConnectIQLifecycleListener()
    private val companionAppId = "9B0A09CFC89E4F7CA5E4AB21400EE424"//fb8c00180889407a913db58884cb3ec3
    private val companionAppCurrentVersion = 1000

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
    fun shutDownConnectIQ(){
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
            if(DEBUG_MODE){
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
                    if (it != null){
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

    private fun decorateDevice(device: IQDevice){
        if(decoratedDevices.contains(device.deviceIdentifier)){
            if(DEBUG_MODE) Log.e(tag, "SHORT CIRCUITING decorating device: ${device.friendlyName}")
            return
        }
        else {
            if(DEBUG_MODE) Log.w(tag, "decorating device: ${device.friendlyName}")
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
            if(device != null && status != null) {
                if (!decoratedDevices.contains(device.deviceIdentifier)) {
                    onDeviceEvent(device, status)
                    decorateDevice(device)
                    decoratedDevices.add(device.deviceIdentifier)
                }else{
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
                        if (app.version() < companionAppCurrentVersion) {
                            // Prompt the user to upgrade
                            if(DEBUG_MODE) {
                                Log.w(tag, "my current version is: $companionAppCurrentVersion")
                                Log.w(tag, "detected app version on $device: ${app.version()}")
                            }
                        }
                        if(DEBUG_MODE) {
                            Log.w(tag, "app.status = ${app.status}")
                            Log.w(tag, "deviceIdentifier: ${device.deviceIdentifier}")
                            Log.w(tag, "device friendlyName: ${device.friendlyName}")
                            Log.w(tag, "device status: ${device.status}")
                        }
                        onAppEvent(device, app.status)
                    }
                }
            }

            override fun onApplicationNotInstalled(applicationId: String) {
                // Prompt user with information
                //val dialog = AlertDialog.Builder(ctx)
                //dialog.setTitle("Missing Application")
                //dialog.setMessage("Corresponding IQ application not installed")
                //dialog.setPositiveButton(ctx.getString("ok"), null)
                //dialog.create().show()
                Log.w(tag, "app not installed on ${device.friendlyName}")
            }
        })
    }
}