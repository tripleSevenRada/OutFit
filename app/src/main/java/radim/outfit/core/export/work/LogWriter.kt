package radim.outfit.core.export.work

import android.support.v7.app.AppCompatActivity
import android.util.Log
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import radim.outfit.LOG_TAG_MAIN
import radim.outfit.core.export.logic.Result
import radim.outfit.debugdumps.writeTextFile
import radim.outfit.getString
import java.io.File

fun writeLog(result: Result, act: AppCompatActivity) {
    //fire and forget writing log file
    act.doAsync {
        var savedOK = true
        try {
            when (result) {
                is Result.Success -> {
                    writeTextFile(File(result.fileDir.absolutePath +
                            File.separatorChar +
                            result.filename +
                            ".DEBUG_MODE.log"
                    ), result.debugMessage)
                }
                is Result.Fail -> {
                    if (result.fileDir != null &&
                            try {
                                result.fileDir.exists()
                            } catch (e: Exception) {
                                false
                            } &&
                            result.filename != null) {
                        val rootPath = result.fileDir.absolutePath + File.separatorChar + result.filename
                        writeTextFile(File("$rootPath.DEBUG_MODE.dump"), result.debugMessage)
                        writeTextFile(File("$rootPath.error.dump"), result.errorMessage)
                    } else {
                        savedOK = false
                        Log.e(LOG_TAG_MAIN, result.errorMessage.toString())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            savedOK = false
        }
        uiThread {
            if (savedOK) act.toast(act.getString("logs_written"))
            else act.toast(act.getString("logs_write_error"))
        }
    }
}