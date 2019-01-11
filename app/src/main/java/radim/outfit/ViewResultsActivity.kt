package radim.outfit

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.util.Log
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.content_stats.*
import android.view.View
import kotlinx.android.synthetic.main.activity_view_results.*
import kotlinx.android.synthetic.main.content_connectiq.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import radim.outfit.core.copyFilesIntoTarget
import radim.outfit.core.emptyTarget
import radim.outfit.core.getListOfExistingFiles
import radim.outfit.core.getListOfFitFilesRecursively
import radim.outfit.core.viewmodels.ViewResultsActivityViewModel
import java.io.File
import kotlin.text.StringBuilder
import java.util.ArrayList

const val NANOHTTPD_SERVE_FROM_DIR_NAME = "nano-httpd-serve-from" // plus xml resources - paths...

class ViewResultsActivity : AppCompatActivity() {

    private val tag = "VIEW_RESULTS"
    private lateinit var parcel: ViewResultsParcel
    private lateinit var connectIQButtonListener: ConnectIQButtonListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_results)

        supportActionBar?.title = getString("activity_view_results_label")

        btnCCIQ.elevation = 4F

        disableExecutive()

        if (intent.hasExtra(EXTRA_MESSAGE_VIEW_RESULTS))
            parcel = intent.getParcelableExtra(EXTRA_MESSAGE_VIEW_RESULTS)

        connectIQButtonListener = ConnectIQButtonListener(
                this,
                ::onStartCIQInit,
                ::onFinnishCIQInit,
                ::bindNanoHTTPD
        )
        btnCCIQ.setOnClickListener(connectIQButtonListener)

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
                        systemOutServedFiles(afterFiles)//TODO
                        viewModel.fileOperationsDone = true
                        enableExecutive()
                    } else {
                        FailGracefullyLauncher().failGracefully(this@ViewResultsActivity, "file operations")
                    }
                }
            }
        } else {
            val afterFiles = getListOfFitFilesRecursively(File(dirToServeFromPath))
            systemOutServedFiles(afterFiles)//TODO
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
        //Log.i(tag, "onStart")

    }

    override fun onResume() {
        super.onResume()
        //Log.i(tag, "onResume")

    }

    override fun onPause() {
        super.onPause()
        //Log.i(tag, "onPause")
    }

    override fun onStop() {
        super.onStop()
        //Log.i(tag, "onStop")
        unbindNanoHTTPD()
        // listener keeps track if connected or not
        connectIQButtonListener.unregisterForDeviceEvents()
        connectIQButtonListener.shutdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        //Log.i(tag, "onDestroy")
    }

    // CALLBACKS START

    // ENABLE / DISABLE EXECUTIVE UI
    private fun enableExecutive() {
        btnCCIQ.isEnabled = true; btnCCIQShareCourse.isEnabled = true; progressBarView.visibility = ProgressBar.INVISIBLE
    }

    private fun disableExecutive() {
        btnCCIQ.isEnabled = false; btnCCIQShareCourse.isEnabled = false; progressBarView.visibility = ProgressBar.VISIBLE
    }

    private fun onStartCIQInit() = let { btnCCIQ.isEnabled = false; progressBarView.visibility =ProgressBar.VISIBLE }
    private fun onFinnishCIQInit() = let { btnCCIQ.isEnabled = false; progressBarView.visibility =ProgressBar.INVISIBLE }


    // CALLBACKS END

    fun shareCourse(v: View?) {
        Log.i(tag, "shareCourse")
        val dirToServeFrom = File(filesDir, NANOHTTPD_SERVE_FROM_DIR_NAME)
        val uris = mutableListOf<Uri?>()
        val fitFilesToServe = getListOfFitFilesRecursively(dirToServeFrom)
        fitFilesToServe.forEach {
            val fileUri: Uri? = try {
                FileProvider.getUriForFile(
                        this@ViewResultsActivity,
                        "radim.outfit.fileprovider",
                        it)
            } catch (e: IllegalArgumentException) {
                Log.e("File Selector",
                        "The selected file can't be shared: $it")
                null
            }
            if (fileUri != null) uris.add(fileUri)
        }
        //TODO Total commander
        val launchIntent = Intent()
        val urisAList = ArrayList<Uri?>()
        if(uris[0] != null)urisAList.add(uris[0])
        Log.i("uris", urisAList.toString())
        launchIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        launchIntent.type = MIME_FIT
        launchIntent.action = Intent.ACTION_SEND
        launchIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, urisAList)
        startActivity(Intent.createChooser(launchIntent, getString("share_single")))


/*        Log.i("URIS", uris.toString())
        if (uris.size > 1) {
            val urisAList = ArrayList<Uri>(uris)
            launchIntent.action = Intent.ACTION_SEND_MULTIPLE
            launchIntent.type = MIME_FIT
            launchIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, urisAList)
            launchIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(Intent.createChooser(launchIntent, getString("share_multiple")))
        } else if (uris.size == 1 && uris[0] != null) {
            val urisAList = ArrayList<Uri>(uris)
            launchIntent.action = Intent.ACTION_SEND
            launchIntent.type = MIME_FIT
            launchIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, urisAList)
            launchIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(Intent.createChooser(launchIntent, getString("share_single")))
        }*/
    }
}
