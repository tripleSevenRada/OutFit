package radim.outfit

import android.arch.lifecycle.ViewModelProviders
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.DialogFragment
import android.support.v4.content.FileProvider
import android.util.Log
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.content_stats.*
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import kotlinx.android.synthetic.main.activity_view_results.*
import kotlinx.android.synthetic.main.content_connectiq.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import radim.outfit.core.share.logic.ConnectIQManager
import radim.outfit.core.share.server.LocalHostServer
import radim.outfit.core.share.server.MIME_FIT
import radim.outfit.core.share.work.*
import radim.outfit.core.timer.SimpleTimer
import radim.outfit.core.timer.Timer
import radim.outfit.core.viewmodels.ViewResultsActivityViewModel
import java.io.File
import kotlin.text.StringBuilder

const val NANOHTTPD_SERVE_FROM_DIR_NAME = "nano-httpd-serve-from" // plus xml resources - paths...
const val NANOHTTPD_PORT = 22333

class ViewResultsActivity : AppCompatActivity(),
        IQAppIsInvalidDialogFragment.IQAppIsInvalidDialogListener {

    //https://drive.google.com/open?id=18Z1mDp_IcV8NQWlSipONdPs1XWaNjsOr

    private val tag = "VIEW_RESULTS"
    private lateinit var parcel: ViewResultsParcel
    private lateinit var connectIQManager: ConnectIQManager
    private var shareFitReady = false
    private val btBroadcastReceiver = BTBroadcastReceiver()

    fun onCheckBoxCCIQClicked(checkboxCCIQ: View) {
        if (checkboxCCIQ is CheckBox) {
            setCheckBoxStatus(checkboxCCIQ.isChecked)
            if (checkboxCCIQ.isChecked) startConnectIQServices()
            else stopConnectIQServices()
        }
    }

    private fun setCheckBoxStatus(status: Boolean) {
        this.getSharedPreferences(
                getString(R.string.main_activity_preferences),
                Context.MODE_PRIVATE).edit().putBoolean(getString("checkbox_cciq"), status).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_results)

        supportActionBar?.title = getString("activity_view_results_label")

        content_connectiqCHCKBOX.isChecked = this.getSharedPreferences(
                getString(R.string.main_activity_preferences),
                Context.MODE_PRIVATE).getBoolean(getString("checkbox_cciq"), true)

        disableExecutive()

        if (intent.hasExtra(EXTRA_MESSAGE_VIEW_RESULTS))
            parcel = intent.getParcelableExtra(EXTRA_MESSAGE_VIEW_RESULTS)

        connectIQManager = ConnectIQManager(
                this,
                ::onStartCIQInit,
                ::onFinnishCIQInit,
                ::onDeviceEvent,
                ::onAppEvent,
                ::onFirstINFITDetected
        )

        if (::parcel.isInitialized) {
            val messagesAsStringBuilder = StringBuilder()
            parcel.messages.forEach { with(messagesAsStringBuilder) { append(it); append("\n") } }
            content_statsTVData.text = messagesAsStringBuilder.toString()
        }
        if (DEBUG_MODE && ::parcel.isInitialized) {
            Log.i(tag, "Circular buffer of export PATHS:")
            parcel.buffer.forEach { Log.i(tag, it) }
            Log.i(tag, "Circular buffer of export MAPPING FROM FILENAMES TO COURSENAMES:")
            Log.i(tag, parcel.fileNameToCourseName.toString())
        }

        val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(btBroadcastReceiver, intentFilter)

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE)
        if(btManager is BluetoothManager){
            val btAdapter = btManager.adapter
            val state = btAdapter.state
            if (state == BluetoothAdapter.STATE_OFF) {
                Log.w("BTBroadcastReceiver", "onCreate BT STATE OFF")
                content_connectiqBTIcon.setImageResource(R.mipmap.ic_bluetooth_disabled_black_36dp)
            }
            else if (state == BluetoothAdapter.STATE_ON) {
                Log.w("BTBroadcastReceiver", "onCreate BT STATE ON")
                content_connectiqBTIcon.setImageResource(R.mipmap.ic_bluetooth_black_36dp)
            }
        }
    }

    //BT
    inner class BTBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) == BluetoothAdapter.STATE_OFF) {
                    Log.w("BTBroadcastReceiver", "BT STATE OFF")
                    content_connectiqBTIcon.setImageResource(R.mipmap.ic_bluetooth_disabled_black_36dp)
                }
                else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) == BluetoothAdapter.STATE_ON) {
                    Log.w("BTBroadcastReceiver", "BT STATE ON")
                    content_connectiqBTIcon.setImageResource(R.mipmap.ic_bluetooth_black_36dp)
                    Handler().postDelayed({stopConnectIQServices()}, 6000)
                    Handler().postDelayed({startConnectIQServices()}, 8000)
                }
            }
        }
    }

    // NANO HTTPD START
    private lateinit var server: LocalHostServer

    private fun bindNanoHTTPD() {
        try {
            val filenamesOnly = mutableListOf<String>()
            parcel.buffer.forEach {
                filenamesOnly.add(it.substring(it.lastIndexOf(File.separator) + 1))
            }
            server = LocalHostServer(NANOHTTPD_PORT,
                    File("${filesDir.absolutePath}${File.separator}$NANOHTTPD_SERVE_FROM_DIR_NAME"),
                    filenamesOnly,
                    parcel.fileNameToCourseName)
            if (DEBUG_MODE) Log.i(tag, "JSONArray, coursenames: ${server.coursenamesAsJSON()}")
            server.start()
        } catch (e: Exception) {
            Log.e(tag, e.localizedMessage)
        }
    }

    private fun unbindNanoHTTPD() {
        if (::server.isInitialized) {
            try {
                server.stop()
            } catch (e: Exception) {
                Log.e(tag, e.localizedMessage)
            }
        }
    }
    // NANO HTTPD END

    override fun onStart() {
        super.onStart()
        val viewModel = getViewModel()

        val dirToServeFromPath = "${filesDir.absolutePath}${File.separator}$NANOHTTPD_SERVE_FROM_DIR_NAME"

        if (!viewModel.fileOperationsDone) {
            var dirToServeCreated = false
            var dataToServeReady = true
            doAsync {
                // prepare dir for LocalHostServer to serve from
                try {
                    if (filesDir != null) {
                        val dirToServeFromFile = File(dirToServeFromPath)
                        dirToServeCreated = dirToServeFromFile.mkdir()
                        if (!emptyTarget(dirToServeFromPath)) dataToServeReady = false
                        val existingFiles = getListOfExistingFiles(parcel.buffer)
                        if (existingFiles.isEmpty()) dataToServeReady = false
                        if (!copyFilesIntoTarget(dirToServeFromPath, existingFiles)) dataToServeReady = false
                    } else dataToServeReady = false
                } catch (e: Exception) {
                    dataToServeReady = false
                }
                uiThread {
                    if (DEBUG_MODE) {
                        Log.i(tag, "dirToServeCreated: $dirToServeCreated")
                        Log.i(tag, "dataToServeReady: $dataToServeReady")
                    }
                    if (dataToServeReady) {
                        val afterFiles = getListOfFitFilesRecursively(File(dirToServeFromPath))
                        viewModel.fileOperationsDone = true
                        viewModel.bufferHead = if (afterFiles.isNotEmpty()) afterFiles[0] else null
                        shareFitReady = true
                        if (content_connectiqCHCKBOX.isChecked) startConnectIQServices() // includes disableExecutive() // START
                        else enableExecutive()
                    } else {
                        FailGracefullyLauncher().failGracefully(this@ViewResultsActivity, "file operations")
                    }
                }
            }
        } else {
            shareFitReady = true
            if (content_connectiqCHCKBOX.isChecked) startConnectIQServices() // includes disableExecutive() // START
            else enableExecutive()
        }
    }
    //override fun onResume() { super.onResume() }
    //override fun onPause() { super.onPause() }

    override fun onStop() {
        super.onStop()
        Log.i(tag, "onStop")
        stopConnectIQServices()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(tag, "onDestroy")
        unregisterReceiver(btBroadcastReceiver)
    }

    private fun startConnectIQServices() {
        Log.w("IQ", "START SERVICES")
        bindNanoHTTPD()
        connectIQManager.startConnectIQ()
    }

    private fun stopConnectIQServices() {
        Log.w("IQ", "STOP SERVICES")
        unbindNanoHTTPD()
        connectIQManager.shutDownConnectIQ()
        if (::indicatorIQTimer.isInitialized) {
            indicatorIQTimer.stop()
            if (::indicatorIQTimerCallback.isInitialized) indicatorIQTimerCallback.restart(View.VISIBLE)
        }
        content_connectiqTVDevicesData.text = ""
    }

    // CALLBACKS START
    private fun enableExecutive() {
        content_connectiqCHCKBOX.isEnabled = true
        //btnCCIQShareCourse.isEnabled = true
        activity_view_resultsPB.visibility = ProgressBar.INVISIBLE
    }

    private fun disableExecutive() {
        content_connectiqCHCKBOX.isEnabled = false
        //btnCCIQShareCourse.isEnabled = false
        activity_view_resultsPB.visibility = ProgressBar.VISIBLE
    }

    // ConnectIQ init loop START
    private fun onStartCIQInit() {
        disableExecutive()
    }

    // called back after cca 10 - 30 ms from startConnectIQServices()
    private fun onFinnishCIQInit() {
        enableExecutive()
        indicatorIQTimerCallback = IndicatorIQTimerCallback()
        indicatorIQTimer = SimpleTimer(220, indicatorIQTimerCallback)
        indicatorIQTimer.start()
    }
    // ConnectIQ init loop END

    // IQAppIsInvalidDialogFragment.IQAppIsInvalidDialogListener IMPL BEGIN
    override fun onDialogPositiveClick(dialog: DialogFragment) {
        Log.i(tag, "onDialogPositiveClick")
        when (getDialogType()) {
            is DialogType.NotInstalled -> connectIQManager.goToTheStore()
            is DialogType.OldVersion -> {
                //TODO
            }
            else -> {
                Log.e(tag, "DialogType == null in onDialogPositiveClick," +
                        " this should never happen")
            }
        }
    }
    override fun onDialogNegativeClick(dialog: DialogFragment) {
        Log.i(tag, "onDialogNegativeClick")
        val prefs = this.getSharedPreferences(
                getString(R.string.main_activity_preferences), Context.MODE_PRIVATE)
        when (getDialogType()) {
            is DialogType.NotInstalled -> prefs.edit()
                    .putBoolean("dialog_app_not_installed_disabled", true).apply()
            is DialogType.OldVersion -> prefs.edit()
                    .putBoolean("dialog_app_old_version_disabled", true).apply()
            is DialogType.HowToInFitInfo -> prefs.edit()
                    .putBoolean("dialog_use_infit_like_this_disabled", true).apply()
            else -> {
                Log.e(tag, "DialogType == null in onDialogNegativeClick," +
                        " this should never happen")
            }
        }
    }
    override fun onDialogNeutralClick(dialog: DialogFragment) {
        Log.i(tag, "onDialogNeutralClick")
        //TODO?
    }

    override fun setDialogVisible(visible: Boolean) = let { getViewModel().dialogShown = visible }
    override fun getDialogVisible(): Boolean = getViewModel().dialogShown
    override fun setDialogType(type: DialogType) = let { getViewModel().dialogType = type }
    override fun getDialogType(): DialogType? = getViewModel().dialogType

    private fun getViewModel(): ViewResultsActivityViewModel =
            ViewModelProviders.of(this).get(ViewResultsActivityViewModel::class.java)
    // IQAppIsInvalidDialogFragment.IQAppIsInvalidDialogListener IMPL END

    private val spannedDeviceDisplay = SpannedDeviceDisplay()

    private fun onDeviceEvent(device: IQDevice, status: IQDevice.IQDeviceStatus) {
        spannedDeviceDisplay.onDeviceEvent(device, status, getViewModel())
        if (DEBUG_MODE) Log.i(tag, "onDeviceEvent display after call ${device.friendlyName} $status")
        content_connectiqTVDevicesData.text = spannedDeviceDisplay.getDisplay()
    }

    private fun onAppEvent(device: IQDevice, status: IQApp.IQAppStatus) {
        spannedDeviceDisplay.onAppEvent(device, status, this)
        if (DEBUG_MODE) Log.i(tag, "onAppEvent display after call ${device.friendlyName} $status ")
        content_connectiqTVDevicesData.text = spannedDeviceDisplay.getDisplay()
    }

    private fun onFirstINFITDetected(device: String){
        val prefs = this.getSharedPreferences(
            getString(R.string.main_activity_preferences), Context.MODE_PRIVATE)
        val dialogDisabled = prefs.getBoolean("dialog_use_infit_like_this_disabled", false)
        if(dialogDisabled) {
            return
        }
        if(getDialogVisible() && getDialogType() is DialogType.HowToInFitInfo){
            return
        }
        val message = "${getString("firstINFITReported")} $device"
        connectIQManager.showHowToInFitDialog(message)
    }
    // CALLBACKS END

    // INDICATOR START
    private lateinit var indicatorIQTimer: SimpleTimer
    private lateinit var indicatorIQTimerCallback: IndicatorIQTimerCallback

    inner class IndicatorIQTimerCallback : Timer.TimerCallback {
        private val views = listOf<View>(
                content_connectiqVIEWIndicator1,// 0
                content_connectiqVIEWIndicator2,
                content_connectiqVIEWIndicator3,
                content_connectiqVIEWIndicator4,
                content_connectiqVIEWIndicator5)// 4
        private var pointer = 0
        override fun tick(): Boolean {
            //increasing style
            if (pointer == 0) restart(View.INVISIBLE)
            views[pointer].visibility = View.VISIBLE
            pointer = getIncreasing(pointer)
            //progress bar style
            //views[pointer].visibility = View.INVISIBLE
            //views[getPrevious(pointer)].visibility = View.VISIBLE
            //pointer = getNext(pointer)
            return true
        }

        //private fun getNext(pointer: Int): Int = if(pointer < (views.size - 1)) pointer + 1 else 0
        //private fun getPrevious(pointer: Int): Int = if(pointer > 0) pointer - 1 else (views.size - 1)
        private fun getIncreasing(pointer: Int): Int = if (pointer < (views.size - 1)) pointer + 1 else 0

        fun restart(visibilityRequired: Int) {
            for (i in 0..views.lastIndex)
                views[i].visibility = visibilityRequired; pointer = 0
        }
    }
    // INDICATOR END

    fun shareCourse(v: View?) {
        if (!shareFitReady) return
        val viewModel = getViewModel()
        val fileToShare: File? = viewModel.bufferHead
        val uriToShare: Uri? = if (fileToShare != null) {
            try {
                FileProvider.getUriForFile(
                        this@ViewResultsActivity,
                        "radim.outfit.fileprovider",
                        fileToShare)
            } catch (e: IllegalArgumentException) {
                Log.e(tag, "The selected file can't be shared. ${e.localizedMessage}")
                null
            }
        } else null
        if (uriToShare != null) {
            val launchIntent = Intent(Intent.ACTION_SEND)
            with(launchIntent) {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                type = MIME_FIT
                putExtra(Intent.EXTRA_STREAM, uriToShare)
            }
            startActivity(Intent.createChooser(launchIntent, getString("share_single")))
        } else {
            Toast.makeText(this, getString("share_error"), Toast.LENGTH_LONG).show()
        }
    }
}
