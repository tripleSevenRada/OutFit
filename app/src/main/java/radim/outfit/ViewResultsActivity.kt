package radim.outfit

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_connectiq.*
import kotlinx.android.synthetic.main.content_stats.*
import radim.outfit.core.services.connectiq.ConnectIQButtonListener

import java.lang.StringBuilder
import android.os.IBinder
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import kotlinx.android.synthetic.main.activity_view_results.*
import radim.outfit.core.services.nanohttpd.NanoHttpdService


const val NANOHTTPD_SERVE_FROM_DIR_NAME = "nano-httpd-serve-from"

class ViewResultsActivity : AppCompatActivity() {

    private val tag = "VIEW_RESULTS"
    private lateinit var parcel: ViewResultsParcel

    private val connectIQbuttonListener = ConnectIQButtonListener(
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
        btnConnectIQ.setOnClickListener (connectIQbuttonListener)

        if(::parcel.isInitialized) {
            val messagesAsStringBuilder = StringBuilder()
            parcel.messages.forEach { with(messagesAsStringBuilder) { append(it); append("\n") } }
            tvViewResults.text = messagesAsStringBuilder.toString()
        }
        if (DEBUG_MODE && ::parcel.isInitialized) {
            parcel.buffer.forEach { Log.i(tag, "Circular buffer of exports: $it") }
        }
        enableExecutive()
    }

    private fun startBoundNanoHTTPD(){
        Log.i(tag, "startBoundNanoHTTPD")
        try {
            val startNanoIntent = Intent(this, NanoHttpdService::class.java)
            startService(startNanoIntent)
            bindService(startNanoIntent, nanoHttpdServiceConnection, Context.BIND_AUTO_CREATE)
            //server = LocalHostServer(9090)
            //server?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
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
        connectIQbuttonListener.unregisterForDeviceEvents()
        connectIQbuttonListener.shutdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(tag, "onDestroy")
        unbindService(nanoHttpdServiceConnection)
    }

    // CALLBACKS START

    // ENABLE / DISABLE EXECUTIVE UI
    private fun enableExecutive() { btnConnectIQ.isEnabled = true; progressBarView.visibility = ProgressBar.INVISIBLE }
    private fun disableExecutive() { btnConnectIQ.isEnabled = false; progressBarView.visibility = ProgressBar.VISIBLE }

    // CALLBACKS END

    private var nanoHttpdBoundService: NanoHttpdService? = null

    private val nanoHttpdServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
        }
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as NanoHttpdService.NanoHttpdBinder
            nanoHttpdBoundService = binder.getService()
        }
    }

}
