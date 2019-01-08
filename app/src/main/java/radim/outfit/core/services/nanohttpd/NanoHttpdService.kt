package radim.outfit.core.services.nanohttpd

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

class NanoHttpdService: Service(){

    private val tag = "NanoHttpdService"

    override fun onCreate() {
        Log.i(tag,"onCreate")
        // The service is being created
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(tag,"onStartCommand")
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i(tag,"onBind")
        return NanoHttpdBinder()
    }

    override fun onDestroy() {
        Log.i(tag,"onDestroy()")
    }

    override fun stopService(name: Intent?): Boolean {
        Log.i(tag,"stopService")
        return true
    }

    inner class NanoHttpdBinder: Binder(){
        fun getService():NanoHttpdService?{
            return this@NanoHttpdService
        }
    }
}

