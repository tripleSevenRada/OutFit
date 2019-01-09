package radim.outfit

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.content_connectiq.*
import kotlinx.android.synthetic.main.content_stats.*

import java.lang.StringBuilder
import android.os.IBinder
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.view.View
import kotlinx.android.synthetic.main.activity_view_results.*
import radim.outfit.core.services.nanohttpd.NanoHttpdService

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
                ::disableExecutive,
                ::bindNanoHTTPD)

        btnContentConnectIQ.setOnClickListener (connectIQButtonListener)

        if(::parcel.isInitialized) {
            val messagesAsStringBuilder = StringBuilder()
            parcel.messages.forEach { with(messagesAsStringBuilder) { append(it); append("\n") } }
            tvContentStatsData.text = messagesAsStringBuilder.toString()
        }
        if (DEBUG_MODE && ::parcel.isInitialized) {
            parcel.buffer.forEach { Log.i(tag, "Circular buffer of exports: $it") }
        }
        enableExecutive()
    }

    // NANO HTTPD START

    private var nanoHttpdBoundService: NanoHttpdService? = null
    private var httpdServiceBound = false

    private val httpdServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            httpdServiceBound = false
        }
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as NanoHttpdService.NanoHttpdBinder
            nanoHttpdBoundService = binder.getService()
            httpdServiceBound = true
            if (DEBUG_MODE) Log.i(tag, "ServiceConnected RESPONSE")
            //TODO tell nanoHttpdBoundService what to do async
        }
    }

    private fun bindNanoHTTPD(){
        if(!httpdServiceBound) {
            if (DEBUG_MODE) Log.i(tag, "ServiceConnected REQUEST")
            try {
                val startIntent = Intent(this, NanoHttpdService::class.java)
                val validConnection = bindService(startIntent, httpdServiceConnection, Context.BIND_AUTO_CREATE)
                if(DEBUG_MODE) {
                    Log.i(tag, "valid connection to httpdServiceConnection: $validConnection")
                    if(DEBUG_MODE)Log.i(tag, "nanoHttpdService binding")
                }
            } catch (e: Exception) {
                Log.e(tag, "bindNanoHTTPD - ERROR")
            }
        }
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
        if(httpdServiceBound){
            unbindService(httpdServiceConnection)
            if(DEBUG_MODE)Log.i(tag, "nanoHttpdService unbinding")
            httpdServiceBound = false
        }
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
    private fun enableExecutive() { btnContentConnectIQ.isEnabled = true; progressBarView.visibility = ProgressBar.INVISIBLE }
    private fun disableExecutive() { btnContentConnectIQ.isEnabled = false; progressBarView.visibility = ProgressBar.VISIBLE }

    // CALLBACKS END

    fun shareCourse(v: View?){
        //Log.i(tag, "shareCourse")
    }

}
