package radim.outfit

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.content_connectiq.*
import kotlinx.android.synthetic.main.content_stats.*

import java.lang.StringBuilder
import android.view.View
import kotlinx.android.synthetic.main.activity_view_results.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File

const val NANOHTTPD_SERVE_FROM_DIR_NAME = "nano-httpd-serve-from"

class ViewResultsActivity : AppCompatActivity() {

    private val tag = "VIEW_RESULTS"
    private lateinit var parcel: ViewResultsParcel

    private lateinit var connectIQButtonListener: ConnectIQButtonListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_results)
        if (intent.hasExtra(EXTRA_MESSAGE_VIEW_RESULTS))
            parcel = intent.getParcelableExtra(EXTRA_MESSAGE_VIEW_RESULTS)

        connectIQButtonListener = ConnectIQButtonListener(
                this,
                ::enableExecutive,
                ::disableExecutive)

        btnContentConnectIQ.setOnClickListener (connectIQButtonListener)

        if(::parcel.isInitialized) {
            val messagesAsStringBuilder = StringBuilder()
            parcel.messages.forEach { with(messagesAsStringBuilder) { append(it); append("\n") } }
            tvContentStatsData.text = messagesAsStringBuilder.toString()
        }
        if (DEBUG_MODE && ::parcel.isInitialized) {
            parcel.buffer.forEach { Log.i(tag, "Circular buffer of exports: $it") }
        }

        var dirToServeCreated = false
        var dataToServeReady = true
        doAsync {
            // prepare dir for LocalHostServer to serve from
            try {
                val internalStorageDir = filesDir
                if(internalStorageDir != null){
                    val dirToServeFromPath = "${internalStorageDir.absolutePath}${File.separator}$NANOHTTPD_SERVE_FROM_DIR_NAME"
                    dirToServeCreated = File(dirToServeFromPath).mkdir()
                    val filesToDelete =
                }else dataToServeReady = false
            } catch (e: Exception){
                dataToServeReady = false
            }
            uiThread {
                enableExecutive()
            }
        }
    }

    // NANO HTTPD START

    private fun bindNanoHTTPD(){

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
    private fun enableExecutive() { btnContentConnectIQShareCourse.isEnabled = true; btnContentConnectIQ.isEnabled = true; progressBarView.visibility = ProgressBar.INVISIBLE }
    private fun disableExecutive() { btnContentConnectIQShareCourse.isEnabled = false; btnContentConnectIQ.isEnabled = false; progressBarView.visibility = ProgressBar.VISIBLE }

    // CALLBACKS END

    fun shareCourse(v: View?){
        //Log.i(tag, "shareCourse")
    }

}
