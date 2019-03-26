package radim.outfit

import android.arch.lifecycle.ViewModelProviders
import android.arch.persistence.room.Room
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
import radim.outfit.core.async.ShallowParcel
import radim.outfit.core.async.getShallow
import radim.outfit.core.persistence.*
import radim.outfit.core.share.logic.ConnectIQManager
import radim.outfit.core.share.logic.getEstimatedDownloadTimeInSeconds
import radim.outfit.core.share.logic.getSpannableDownloadInfo
import radim.outfit.core.share.server.LocalHostServer
import radim.outfit.core.share.server.MIME_FIT
import radim.outfit.core.share.work.*
import radim.outfit.core.timer.SimpleTimer
import radim.outfit.core.timer.Timer
import radim.outfit.core.viewmodels.ViewResultsActivityViewModel
import java.io.File

const val NANOHTTPD_SERVE_FROM_DIR_NAME = "nano-httpd-serve-from" // plus xml resources - paths...
const val NANOHTTPD_PORT = 22333

const val BT_ICON_ALPHA = 100
const val DOWNLOAD_TIME_EST_TO_REPORT = 10

class ViewResultsActivity : AppCompatActivity(),
        IQAppIsInvalidDialogFragment.IQAppIsInvalidDialogListener,
        ExplainTrackTools.OnFragmentInteractionListener {

    //https://drive.google.com/open?id=18Z1mDp_IcV8NQWlSipONdPs1XWaNjsOr

    private val tag = "VIEW_RESULTS"
    private lateinit var connectIQManager: ConnectIQManager
    private val btBroadcastReceiver = BTBroadcastReceiver()

    private var shareFitReady = false

    private val idToStatus: MutableMap<Long, IQDevice.IQDeviceStatus> = mutableMapOf()

    private lateinit var parcel: ViewResultsParcel

    // Fragment interaction
    override fun onFragmentInteraction(interaction: Int) {}

    // Menu START
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.view_results, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.explain_waypoints -> {
                startActivity(Intent(this, ExplainWaypointsActivity::class.java))
                true
            }
            R.id.share_standard -> {
                if (::parcel.isInitialized) shareCourse(parcel.getShallow())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    // Menu END

    fun onCheckBoxCCIQClicked(checkboxCCIQ: View) {
        if (checkboxCCIQ is CheckBox) {
            setCheckBoxStatus(checkboxCCIQ.isChecked)
            if (checkboxCCIQ.isChecked && ::parcel.isInitialized) startConnectIQServices(parcel.getShallow())
            else stopConnectIQServices()
        }
    }

    private var disableCheckBoxStatusPersistence = false
    private fun setCheckBoxStatus(status: Boolean) {
        if (disableCheckBoxStatusPersistence) return
        this.getSharedPreferences(
                getString(R.string.main_activity_preferences),
                Context.MODE_PRIVATE).edit().putBoolean(getString("checkbox_cciq"), status).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_results)

        supportActionBar?.title = getString("activity_view_results_label")

        content_connectiqTVTextBoxInfo.text = this.getString("ciq_service_off")
        content_connectiqTVTextBoxInfo.setTextColor(Color.GRAY)

        disableExecutive()

        initSharedPrefs(this)

        if (!permWriteIsGranted(this)) requestPermWrite(this)

        val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(btBroadcastReceiver, intentFilter)

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE)
        content_connectiqBTIcon.imageAlpha = BT_ICON_ALPHA
        if (btManager is BluetoothManager) {
            val btAdapter = btManager.adapter
            val state = btAdapter.state
            if (state == BluetoothAdapter.STATE_OFF) {
                Log.w("BTBroadcastReceiver", "onCreate BT STATE OFF")
                content_connectiqBTIcon.setImageResource(R.mipmap.ic_bluetooth_disabled_black_24dp)
            } else if (state == BluetoothAdapter.STATE_ON) {
                Log.w("BTBroadcastReceiver", "onCreate BT STATE ON")
                content_connectiqBTIcon.setImageResource(R.mipmap.ic_bluetooth_black_24dp)
            }
        }

        connectIQManager = ConnectIQManager(
                this,
                ::onStartCIQInit,
                ::onFinnishCIQInit,
                ::onDeviceEvent,
                ::onAppEvent,
                ::onFirstINFITDetected
        )

        if (intent.hasExtra(EXTRA_MESSAGE_VIEW_RESULTS)) {
            parcel = intent.getParcelableExtra(EXTRA_MESSAGE_VIEW_RESULTS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_PERM_WRITE_EXTERNAL -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay!
                } else {
                    // permission denied, boo!
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun buildStats(shallowParcel: ShallowParcel): SpannableStringBuilder {
        val messagesAsSpannableStringBuilder = SpannableStringBuilder()
        if (shallowParcel.type == ViewResultsParcel.Type.REGULAR) {
            val bufferHead = File(shallowParcel.buffer[0])

            if (bufferHead.exists()) {
                val secondsTotal = getEstimatedDownloadTimeInSeconds(bufferHead.length())
                if (secondsTotal > DOWNLOAD_TIME_EST_TO_REPORT) {
                    with(messagesAsSpannableStringBuilder) {
                        append(getSpannableDownloadInfo(secondsTotal, this@ViewResultsActivity))
                        append("\n")
                    }
                }
                shallowParcel.messages.forEach { with(messagesAsSpannableStringBuilder) { append(it); append("\n") } }
            }

            messagesAsSpannableStringBuilder.append("\n${this.getString("courses")}\n")
            val existingFiles = getListOfExistingFiles(shallowParcel.buffer.toTypedArray())
            existingFiles.forEach {
                messagesAsSpannableStringBuilder.append(shallowParcel.fileNameToCoursename[it.name]
                        ?: "?").append("\n")
            }
            return messagesAsSpannableStringBuilder
        }
        if (shallowParcel.type != ViewResultsParcel.Type.REGULAR) {
            shallowParcel.messages.forEach { with(messagesAsSpannableStringBuilder) { append(it); append("\n") } }
            return messagesAsSpannableStringBuilder
        }
        messagesAsSpannableStringBuilder.append("?")
        return messagesAsSpannableStringBuilder
    }

    private fun populateStats(builder: SpannableStringBuilder) {
        content_statsTVData.text = builder
    }

    //BT
    inner class BTBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) == BluetoothAdapter.STATE_OFF) {
                    Log.w("BTBroadcastReceiver", "BT STATE OFF")
                    content_connectiqBTIcon.setImageResource(R.mipmap.ic_bluetooth_disabled_black_24dp)
                } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) == BluetoothAdapter.STATE_ON) {
                    Log.w("BTBroadcastReceiver", "BT STATE ON")
                    content_connectiqBTIcon.setImageResource(R.mipmap.ic_bluetooth_black_24dp)
                }
            }
        }
    }

    // NANO HTTPD START
    private lateinit var server: LocalHostServer

    private fun bindNanoHTTPD(filenamesExisting: List<String>, shallowParcel: ShallowParcel): BindNanoHTTPDResult {

        if (filenamesExisting.isEmpty()) return BindNanoHTTPDResult.BindNanoFailure(getString("zero_length_buffer"))

        unbindNanoHTTPD()

        try {
            server = LocalHostServer(NANOHTTPD_PORT,
                    File("${filesDir.absolutePath}${File.separator}$NANOHTTPD_SERVE_FROM_DIR_NAME"),
                    filenamesExisting,
                    shallowParcel.fileNameToCoursename)
            if (DEBUG_MODE) Log.i(tag, "JSONArray, coursenames: ${server.coursenamesAsJSON()}")
            server.start()

        } catch (e: Exception) {
            Log.e(tag, e.localizedMessage)
            return BindNanoHTTPDResult.BindNanoFailure(getString("ciqservice_generic_error"))
        }
        return BindNanoHTTPDResult.BindNanoSuccess(filenamesExisting.size)
    }

    sealed class BindNanoHTTPDResult {
        data class BindNanoSuccess(val size: Int) : BindNanoHTTPDResult()
        data class BindNanoFailure(val why: String) : BindNanoHTTPDResult()
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


    //
    //
    //
    override fun onStart() {
        super.onStart()
        Log.e(tag, "onStart")

        // fire and forget writing stats to ui
        fun doStats(shallowParcel: ShallowParcel) {
            doAsync {
                val builder = buildStats(shallowParcel)
                uiThread { populateStats(builder) }
            }
        }

        val viewModel = getViewModel()
        val dirToServeFromPath = "${filesDir.absolutePath}${File.separator}$NANOHTTPD_SERVE_FROM_DIR_NAME"

        if (DEBUG_MODE) {
            Log.w(tag, "viewModel.fileOpsDone ${viewModel.fileOpsDone}")
            Log.w(tag, "::parcel.isInitialized ${::parcel.isInitialized}")
            Log.w(tag, "viewModel.parcelPersistenceDone ${viewModel.parcelPersistenceDone}")
        }

        // only the very first entry of regular parcel (got from previous (main) activity)
        // OR
        // every entry if in standalone mode (no parcel in the intent)
        // TODO
        // -- parcel is not a property of view model.
        if (!viewModel.fileOpsDone ||
                !::parcel.isInitialized ||
                !viewModel.parcelPersistenceDone) {

            var dataToServeReady = true

            doAsync {

                // parcel persistence operations
                // every entry if in standalone mode - parcel is not initialized
                if (!viewModel.parcelPersistenceDone ||
                        !::parcel.isInitialized) {

                    var db: ParcelDatabase? = null
                    try {
                        db = Room.databaseBuilder(
                                applicationContext,
                                ParcelDatabase::class.java, "parcel-database"
                        ).build()

                        if (::parcel.isInitialized && parcel.type == ViewResultsParcel.Type.REGULAR) {// then persist it
                            db.parcelDao().persist(parcel.getEntity())
                            viewModel.parcelPersistenceDone = true
                        } else {
                            val parcelEntities: List<ViewResultsParcelEntity> = db.parcelDao().retrieve()
                            parcel = if (parcelEntities.isNotEmpty())
                                parcelEntities[0].getParcel()
                            else {
                                getDefaultParcel(this@ViewResultsActivity)
                            }
                        }
                    } catch (e: java.lang.Exception) {
                        parcel = getErrorParcel(this@ViewResultsActivity)
                    }
                    db?.close()
                }

                // prepare dir for LocalHostServer to serve from
                try {
                    if (filesDir != null && ::parcel.isInitialized) {
                        if (!viewModel.fileOpsDone && parcel.type == ViewResultsParcel.Type.REGULAR) {

                            File(dirToServeFromPath).mkdir()
                            dataToServeReady = emptyTarget(dirToServeFromPath)
                            val existingFiles = getListOfExistingFiles(parcel.buffer)
                            if (dataToServeReady) dataToServeReady = copyFilesIntoTarget(dirToServeFromPath, existingFiles)
                            Log.e(tag, "data to serve ready: $dataToServeReady")
                        }
                    } else dataToServeReady = false
                } catch (e: Exception) {
                    dataToServeReady = false
                }

                uiThread {

                    if (parcel.type != ViewResultsParcel.Type.REGULAR) {
                        disableCheckBoxStatusPersistence = true
                        content_connectiqCHCKBOX.isChecked = false
                        enableExecutive()
                        if (parcel.type == ViewResultsParcel.Type.DEFAULT) {
                            //TODO
                        }
                        //returns
                    } else {
                        lookUpConnectiqCHCKBOX()

                        if (dataToServeReady && parcel.type == ViewResultsParcel.Type.REGULAR) {
                            viewModel.fileOpsDone = true
                            shareFitReady = true
                            if (content_connectiqCHCKBOX.isChecked) startConnectIQServices(parcel.getShallow()) // includes disableExecutive() // START
                            else enableExecutive()
                        } else {
                            FailGracefullyLauncher().failGracefully(this@ViewResultsActivity, "file operations error")
                            finish()
                        }
                    }
                    doStats(parcel.getShallow())
                }
            }
        } else {
            doStats(parcel.getShallow())
            lookUpConnectiqCHCKBOX()
            shareFitReady = true
            if (content_connectiqCHCKBOX.isChecked)
                startConnectIQServices(parcel.getShallow()) // includes disableExecutive()
            else enableExecutive()
        }
    }
    //
    //
    //


    private fun lookUpConnectiqCHCKBOX() {
        content_connectiqCHCKBOX.isChecked = this@ViewResultsActivity.getSharedPreferences(
                getString(R.string.main_activity_preferences),
                Context.MODE_PRIVATE).getBoolean(getString("checkbox_cciq"), true)
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

    private fun startConnectIQServices(shallowParcel: ShallowParcel) {
        Log.w("IQ", "START SERVICES")

        val filenamesExisting = mutableListOf<String>()

        doAsync {
            shallowParcel.buffer.forEach {
                if (File(it).exists())
                    filenamesExisting.add(it.substring(it.lastIndexOf(File.separator) + 1))
            }

            uiThread {
                val bindResult = bindNanoHTTPD(filenamesExisting, shallowParcel)
                when (bindResult) {
                    is BindNanoHTTPDResult.BindNanoSuccess -> {
                        connectIQManager.startConnectIQ()
                        if (DEBUG_MODE) Log.i(tag, "Bind result: served files.size: ${bindResult.size}")
                    }
                    is BindNanoHTTPDResult.BindNanoFailure -> {
                        content_connectiqTVTextBoxInfo.text = bindResult.why
                        content_connectiqTVTextBoxInfo.setTextColor(Color.RED)
                        enableExecutive()
                    }
                }
            }
        }
    }

    private fun stopConnectIQServices() {
        Log.w("IQ", "STOP SERVICES")
        unbindNanoHTTPD()
        connectIQManager.shutDownConnectIQ()
        if (::indicatorIQTimer.isInitialized) {
            indicatorIQTimer.stop()
            if (::indicatorIQTimerCallback.isInitialized) indicatorIQTimerCallback.restart(View.VISIBLE)
        }
        content_connectiqTVTextBoxInfo.text = getString("ciq_service_off")
        content_connectiqTVTextBoxInfo.setTextColor(Color.GRAY)
        content_connectiqTVDevicesData.text = ""
        textBoxInfoTouched = false
        idToStatus.clear()
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

    // device event comes sooner!
    private var textBoxInfoTouched = false

    // called back after cca 10 - 30 ms from startConnectIQServices()
    private fun onFinnishCIQInit() {
        enableExecutive()
        indicatorIQTimerCallback = IndicatorIQTimerCallback()
        indicatorIQTimer = SimpleTimer(220, indicatorIQTimerCallback)
        indicatorIQTimer.start()
        if (!textBoxInfoTouched) {
            content_connectiqTVTextBoxInfo.text = getString("waiting_for_a_device_connected")
            content_connectiqTVTextBoxInfo.setTextColor(Color.GRAY)
        }
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
    private fun getHowToInfitThisIncarnation() = getViewModel().dialogHowToInfitIncarnation
    private fun setHowToInfitThisIncarnation() {
        getViewModel().dialogHowToInfitIncarnation = true
    }

    private fun getViewModel(): ViewResultsActivityViewModel =
            ViewModelProviders.of(this).get(ViewResultsActivityViewModel::class.java)
    // IQAppIsInvalidDialogFragment.IQAppIsInvalidDialogListener IMPL END

    private val spannedDeviceDisplay = SpannedDeviceDisplay()

    private fun onDeviceEvent(device: IQDevice, status: IQDevice.IQDeviceStatus) {
        // any event

        // IQDevice.IQDeviceStatus.NOT_CONNECTED
        // IQDevice.IQDeviceStatus.CONNECTED
        // IQDevice.IQDeviceStatus.NOT_PAIRED
        // IQDevice.IQDeviceStatus.UNKNOWN

        if (DEBUG_MODE) Log.i(tag, "onDeviceEvent -- ${device.hashCode()} " +
                "-- ${device.deviceIdentifier} -- name >${device.friendlyName}< -- ${status}")

        val memoizedFriendlyNameOrNull: String? = getViewModel().idToFriendlyName[device.deviceIdentifier]
        // https://improve-future.com/en/kotlin-difference-between-blank-and-empty.html
        val friendlyNameOrNull: String? =
                if (with(device.friendlyName) { isBlank() || isEmpty() }) memoizedFriendlyNameOrNull
                else device.friendlyName
        // update
        if (friendlyNameOrNull != null)
            getViewModel().idToFriendlyName[device.deviceIdentifier] = friendlyNameOrNull
        idToStatus[device.deviceIdentifier] = status

        val connectedEntries = idToStatus.filter { it.value == IQDevice.IQDeviceStatus.CONNECTED }
        if (connectedEntries.isNotEmpty()) {
            // compose message
            val builder = StringBuilder()
            builder.append(getString("how_to_use_infit"))
            var count = 0
            val last = connectedEntries.size - 1
            connectedEntries.forEach {
                val friendlyName = getViewModel().idToFriendlyName[it.key] ?: getString("device")
                builder.append(" $friendlyName")
                if (count < last) builder.append(",")
                count++
            }
            content_connectiqTVTextBoxInfo.text = builder.toString()
            content_connectiqTVTextBoxInfo.setTextColor(Color.GREEN)
            textBoxInfoTouched = true
        } else {
            content_connectiqTVTextBoxInfo.text = getString("waiting_for_a_device_connected")
            content_connectiqTVTextBoxInfo.setTextColor(Color.GRAY)
            textBoxInfoTouched = true
        }

        spannedDeviceDisplay.onDeviceEvent(device, status, getViewModel(), getString("device"))
        if (DEBUG_MODE) Log.i(tag, "onDeviceEvent display after call ${device.friendlyName} $status")
        content_connectiqTVDevicesData.text = spannedDeviceDisplay.getDisplay()
    }

    private fun onAppEvent(device: IQDevice, status: IQApp.IQAppStatus) {
        spannedDeviceDisplay.onAppEvent(device, status, this)
        if (DEBUG_MODE) Log.i(tag, "onAppEvent display after call ${device.friendlyName} $status ")
        content_connectiqTVDevicesData.text = spannedDeviceDisplay.getDisplay()
    }

    private fun onFirstINFITDetected(device: String) {
        val prefs = this.getSharedPreferences(
                getString(R.string.main_activity_preferences), Context.MODE_PRIVATE)
        val dialogDisabled = prefs.getBoolean("dialog_use_infit_like_this_disabled", false)
        if (dialogDisabled) return
        if (getDialogVisible() && getDialogType() is DialogType.HowToInFitInfo) return
        if (getHowToInfitThisIncarnation()) return
        setHowToInfitThisIncarnation()
        val message = "${getString("how_to_use_infit")} $device"
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

    private fun shareCourse(shallowParcel: ShallowParcel) {
        if (!shareFitReady) return
        doAsync {

            val afterFiles: MutableList<File> = mutableListOf()
            shallowParcel.buffer.forEach {
                val file = File(it); if (file.exists()) afterFiles.add(file)
            }
            val bufferHeadExternalStorage = if (afterFiles.isNotEmpty()) afterFiles[0] else null
            var bufferHeadInternalStorage =
                    if (bufferHeadExternalStorage != null)
                        File("${filesDir.absolutePath}${File.separator}$NANOHTTPD_SERVE_FROM_DIR_NAME" +
                                "${File.separator}${bufferHeadExternalStorage.name}") else null
            if (bufferHeadInternalStorage != null && !bufferHeadInternalStorage.exists())
                bufferHeadInternalStorage = null

            uiThread {

                val uriToShare: Uri? =
                        if (bufferHeadInternalStorage != null) {
                            try {
                                FileProvider.getUriForFile(
                                        this@ViewResultsActivity,
                                        "radim.outfit.fileprovider",
                                        bufferHeadInternalStorage)
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
                    Toast.makeText(this@ViewResultsActivity,
                            "${this@ViewResultsActivity.getString("sharing_buffer_head")} " +
                                    "${bufferHeadInternalStorage?.name}", Toast.LENGTH_LONG).show()
                    startActivity(Intent.createChooser(launchIntent, getString("share_single")))
                }
            }
        }
    }
}
