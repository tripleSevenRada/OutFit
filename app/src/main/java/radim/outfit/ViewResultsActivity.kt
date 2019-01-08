package radim.outfit

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import fi.iki.elonen.NanoHTTPD
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_view_results.*
import kotlinx.android.synthetic.main.content_connectiq.*
import kotlinx.android.synthetic.main.content_stats.*
import radim.outfit.core.services.services.connectiq.ConnectIQButtonListener
import radim.outfit.core.services.services.nanohttpd.LocalHostServer
import java.lang.StringBuilder

const val NANOHTTPD_SERVE_FROM_DIR_NAME = "nano-httpd-serve-from"

class ViewResultsActivity : AppCompatActivity() {

    private val tag = "VIEW_RESULTS"
    private lateinit var parcel: ViewResultsParcel

    private val connectIQ = ConnectIQButtonListener(
            this,
            ::enableExecutive,
            ::disableExecutive,
            ::startBoundNanoHTTPD)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(tag, "onCreate")
        setContentView(R.layout.activity_view_results)
        if (intent.hasExtra(EXTRA_MESSAGE_VIEW_RESULTS))
            parcel = intent.getParcelableExtra(EXTRA_MESSAGE_VIEW_RESULTS)

        tvViewResultsLabel.text = getString("stats_label")
        btnConnectIQ.setOnClickListener (connectIQ)

        if(::parcel.isInitialized) {
            val messagesAsStringBuilder = StringBuilder()
            parcel.messages.forEach { with(messagesAsStringBuilder) { append(it); append("\n") } }
            tvViewResults.text = messagesAsStringBuilder.toString()
        }
        if (DEBUG_MODE && ::parcel.isInitialized) {
            parcel.buffer.forEach { Log.i(tag, "Circular buffer of exports: $it") }
        }
        progressBarView.visibility = ProgressBar.INVISIBLE
    }

    private var server: LocalHostServer? = null
    private fun startBoundNanoHTTPD(){
        Log.i(tag, "startBoundNanoHTTPD")
        try {
            server = LocalHostServer(9090)
            server?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        } catch (e: Exception){
            Log.e(tag, "startBoundNanoHTTPD - ERROR")
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i(tag, "onStart")

    }

    override fun onResume() {
        super.onResume()
        Log.i(tag, "onResume")

    }

    override fun onPause() {
        super.onPause()
        Log.i(tag, "onPause")

    }

    override fun onStop() {
        super.onStop()
        Log.i(tag, "onStop")
        connectIQ.unregisterForDeviceEvents()
        connectIQ.shutdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(tag, "onDestroy")
        server?.stop()
    }

    // CALLBACKS START

    // ENABLE / DISABLE EXECUTIVE UI
    private fun enableExecutive() = run { btnConnectIQ.isEnabled = true; progressBar.visibility = ProgressBar.VISIBLE }
    private fun disableExecutive() = run { btnConnectIQ.isEnabled = false; progressBar.visibility = ProgressBar.INVISIBLE }

    // CALLBACKS END

}
