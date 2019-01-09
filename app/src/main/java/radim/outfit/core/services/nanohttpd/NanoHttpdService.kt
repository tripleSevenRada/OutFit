package radim.outfit.core.services.nanohttpd

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import fi.iki.elonen.NanoHTTPD

class NanoHttpdService : Service() {

    private val tag = "NanoHttpdService"
    private val binder = NanoHttpdBinder()
    private lateinit var server: LocalHostServer
    private var running = false

    override fun onCreate() {
        super.onCreate()
        Log.i(tag, "onCreate")
        server = LocalHostServer(2222)
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i(tag, "onBind")
        if (!running && ::server.isInitialized) {
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            running = true
            Log.i(tag, "LOCALHOST START")
        }
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(tag, "onUnbind")
        if (running && ::server.isInitialized) {
            server.stop()
            running = false
            Log.i(tag, "LOCALHOST STOP")
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(tag, "onDestroy")
    }

    inner class NanoHttpdBinder : Binder() {
        fun getService(): NanoHttpdService {
            return this@NanoHttpdService
        }
    }
}

