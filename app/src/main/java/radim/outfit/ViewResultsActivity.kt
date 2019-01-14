package radim.outfit

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.util.Log
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.content_stats.*
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import com.garmin.android.connectiq.IQDevice
import kotlinx.android.synthetic.main.activity_view_results.*
import kotlinx.android.synthetic.main.content_connectiq.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import radim.outfit.core.share.work.copyFilesIntoTarget
import radim.outfit.core.share.work.emptyTarget
import radim.outfit.core.share.work.getListOfExistingFiles
import radim.outfit.core.share.work.getListOfFitFilesRecursively
import radim.outfit.core.share.logic.ConnectIQManager
import radim.outfit.core.share.server.MIME_FIT
import radim.outfit.core.timer.SimpleTimer
import radim.outfit.core.timer.Timer
import radim.outfit.core.viewmodels.ViewResultsActivityViewModel
import java.io.File
import kotlin.text.StringBuilder

const val NANOHTTPD_SERVE_FROM_DIR_NAME = "nano-httpd-serve-from" // plus xml resources - paths...

class ViewResultsActivity : AppCompatActivity() {

    private val tag = "VIEW_RESULTS"
    private lateinit var parcel: ViewResultsParcel
    private lateinit var connectIQManager: ConnectIQManager


    fun onCheckBoxCCIQClicked(checkboxCCIQ: View) {
        if (checkboxCCIQ is CheckBox) {
            setCheckBoxStatus(checkboxCCIQ.isChecked)
            if(checkboxCCIQ.isChecked) startConnectIQServices()
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

        chbCCIQ.isChecked = this.getSharedPreferences(
                getString(R.string.main_activity_preferences),
                Context.MODE_PRIVATE).getBoolean(getString("checkbox_cciq"), true)

        disableExecutive()

        if (intent.hasExtra(EXTRA_MESSAGE_VIEW_RESULTS))
            parcel = intent.getParcelableExtra(EXTRA_MESSAGE_VIEW_RESULTS)

        connectIQManager = ConnectIQManager(
                this,
                ::onStartCIQInit,
                ::onFinnishCIQInit,
                ::onDeviceEvent
        )

        if (::parcel.isInitialized) {
            val messagesAsStringBuilder = StringBuilder()
            parcel.messages.forEach { with(messagesAsStringBuilder) { append(it); append("\n") } }
            tvContentStatsData.text = messagesAsStringBuilder.toString()
        }
        if (DEBUG_MODE && ::parcel.isInitialized) {
            parcel.buffer.forEach { Log.i(tag, "Circular buffer of exports: $it") }
        }

        val viewModel = ViewModelProviders.of(this).get(ViewResultsActivityViewModel::class.java)

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
                        if (DEBUG_MODE) systemOutServedFiles(afterFiles)
                        viewModel.fileOperationsDone = true
                        viewModel.bufferHead = if (afterFiles.isNotEmpty()) afterFiles[0] else null
                        if(chbCCIQ.isChecked)startConnectIQServices() // includes disableExecutive()
                        else enableExecutive()
                    } else {
                        FailGracefullyLauncher().failGracefully(this@ViewResultsActivity, "file operations")
                    }
                }
            }
        } else {
            if (DEBUG_MODE) {
                val afterFiles = getListOfFitFilesRecursively(File(dirToServeFromPath))
                systemOutServedFiles(afterFiles)
            }
            enableExecutive()
        }
    }

    private fun systemOutServedFiles(files: List<File>) {
        files.forEach { System.out.println("SERVED QUEUE: $it") }
    }

    // NANO HTTPD START
    private fun bindNanoHTTPD() {

    }

    private fun unbindNanoHTTPD() {

    }
    // NANO HTTPD END

    override fun onStart() {
        super.onStart()
        if(::connectIQManager.isInitialized && chbCCIQ.isChecked) startConnectIQServices()
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
    }

    private fun startConnectIQServices(){
        bindNanoHTTPD()
        connectIQManager.startConnectIQ()
    }
    private fun stopConnectIQServices(){
        unbindNanoHTTPD()
        // listener keeps track if connected or not
        connectIQManager.shutDownConnectIQ()
        if (::indicatorIQTimer.isInitialized) {
            indicatorIQTimer.stop()
            if (::indicatorIQTimerCallback.isInitialized) indicatorIQTimerCallback.restart(View.VISIBLE)
        }
        enableExecutive()
    }

    // CALLBACKS START
    private fun enableExecutive() {
        chbCCIQ.isEnabled = true
        //btnCCIQShareCourse.isEnabled = true
        progressBarView.visibility = ProgressBar.INVISIBLE
    }
    private fun disableExecutive() {
        chbCCIQ.isEnabled = false
        //btnCCIQShareCourse.isEnabled = false
        progressBarView.visibility = ProgressBar.VISIBLE
    }

    //ConnectIQ init loop START
    private fun onStartCIQInit() {
        disableExecutive()
    }
    //called back after cca 10 - 30 ms from startConnectIQServices()
    private fun onFinnishCIQInit() {
        enableExecutive()
        indicatorIQTimerCallback = IndicatorIQTimerCallback()
        indicatorIQTimer = SimpleTimer(220, indicatorIQTimerCallback)
        indicatorIQTimer.start()
    }
    //ConnectIQ init loop END

    private val deviceLogBuilder = StringBuilder()
    private fun onDeviceEvent(device: IQDevice, status: IQDevice.IQDeviceStatus) {
        //TODO
        deviceLogBuilder.append("${device.deviceIdentifier} ${device.friendlyName} ${device.status} $status\n")
        tvCCIQDevicesData.text = deviceLogBuilder.toString()
    }
    // CALLBACKS END

    // INDICATOR START
    private lateinit var indicatorIQTimer: SimpleTimer
    private lateinit var indicatorIQTimerCallback: IndicatorIQTimerCallback

    inner class IndicatorIQTimerCallback : Timer.TimerCallback {
        private val views = listOf<View>(
                CCIQIndicatorView1,// 0
                CCIQIndicatorView2,
                CCIQIndicatorView3,
                CCIQIndicatorView4,
                CCIQIndicatorView5)// 4
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
        val viewModel = ViewModelProviders.of(this).get(ViewResultsActivityViewModel::class.java)
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
