package radim.outfit.core.share.logic

import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQDevice
import radim.outfit.DEBUG_MODE
import radim.outfit.FailGracefullyLauncher

class ConnectIQButtonListener(
        private val ctx: AppCompatActivity,
        private val onStartInit: () -> Unit,
        private val onFinishInit: () -> Unit,
        private val onDeviceEvent: (IQDevice, String) -> Unit,
        private val bindNanoHTTPD: () -> Unit
) : View.OnClickListener {

    private val tag = "ConnIQList"
    private val connectionType = ConnectIQ.IQConnectType.TETHERED
    private val connectIQ: ConnectIQ = ConnectIQ.getInstance(ctx, connectionType)
    private val connectIQListener: ConnectIQ.ConnectIQListener = ConnectIQLifecycleListener()
    //val myApp = "9B0A09CFC89E4F7CA5E4AB21400EE424"//fb8c00180889407a913db58884cb3ec3

    private var connectIQIsInitialized = false
    private var connectIQIsBeingInitialized = false

    override fun onClick(v: View?) {
        if (!connectIQIsInitialized &&
                !connectIQIsBeingInitialized) {
            connectIQIsBeingInitialized = true
            onStartInit()
            Log.i(tag, "init")
            connectIQ.initialize(ctx, true, connectIQListener)
        }
    }

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
            bindNanoHTTPD()
            connectIQIsInitialized = true
            connectIQIsBeingInitialized = false
            // Do any post initialization setup.
            Log.i(tag, "onSdkReady")
            // list connected devices
            val devices = connectIQ.connectedDevices
            if (devices != null && devices.size > 0) {
                devices.forEach {
                    if (DEBUG_MODE) {
                        Log.i(tag, "CONNECTED!DEVICE: deviceIdentifier: ${it?.deviceIdentifier}")
                        Log.i(tag, "CONNECTED!DEVICE: friendlyName: ${it?.friendlyName}")
                        Log.i(tag, "CONNECTED!DEVICE: status: ${it?.status}")
                        Log.i(tag, "CONNECTED!DEVICE: describeContents(): ${it?.describeContents()}")
                    }
                    // Register to receive status updates
                    if (it != null){
                        // send callback to ViewResultsActivity
                        onDeviceEvent(it," onSdkReady")
                        connectIQ.registerForDeviceEvents(it, DeviceEventListener())
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

    inner class DeviceEventListener : ConnectIQ.IQDeviceEventListener {
        override fun onDeviceStatusChanged(p0: IQDevice?, p1: IQDevice.IQDeviceStatus?) {
            if (DEBUG_MODE) {
                Log.i(tag, "STATUS_CHANGED, deviceIdentifier: ${p0?.deviceIdentifier}")
                Log.i(tag, "STATUS_CHANGED, friendlyName: ${p0?.friendlyName}")
                Log.i(tag, "STATUS_CHANGED, newStatus: ${p1?.toString()}")
            }
            val message = if(p1 != null) p1.toString() else " null"
            if(p0 != null)
            onDeviceEvent(p0, message)
        }
    }

    fun unregisterForDeviceEvents() {
        if (connectIQIsInitialized) {
            connectIQ.connectedDevices?.forEach {
                if (it != null) connectIQ.unregisterForDeviceEvents(it)
            }
        }
    }

    fun shutdown() {
        if (connectIQIsInitialized) connectIQ.shutdown(ctx)
        connectIQIsInitialized = false
    }
}