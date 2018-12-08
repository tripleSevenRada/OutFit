package radim.outfit.core.export.logic

import android.util.Log
import android.view.View
import android.widget.EditText
import locus.api.objects.extra.Track
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import radim.outfit.core.export.work.locusapiextensions.isTimestamped
import radim.outfit.core.getFilename
import java.io.File


// error codes 8 - 14
// https://drive.google.com/file/d/1wwYzoPQts1HreDpS614oMAVyafU07ZYF/view?usp=sharing

class ExportListener(
        private val execute: (File?, String?, Track?, Float) -> Result,
        var exportPOJO: ExportPOJO,
        private val onFinishCallback: (Result) -> Unit,
        private val onStartCallback: () -> Unit,
        private val showSpeedPickerDialog: () -> Unit
) : View.OnClickListener {

    private val tag = "ExportListener"

    private lateinit var editTextFilename: EditText
    private lateinit var defaultFilename: String

    @Throws(java.lang.RuntimeException::class)
    fun attachView(v: View) {
        if (v is EditText) editTextFilename = v else throw RuntimeException("EditTextOnly")
    }

    fun attachDefaultFilename(filename: String) {
        this.defaultFilename = filename
    }

    override fun onClick(v: View) {

        if (!isDataNonNull()) return
        val track = exportPOJO.track
        track ?: return

        // https://medium.com/coding-blocks/making-asynctask-obsolete-with-kotlin-5fe1d944d69
        // https://antonioleiva.com/anko-background-kotlin-android/

        doAsync {
            val trackIsFullyTimestamped = track.isTimestamped()
            uiThread {
                Log.i(tag, "trackIsFullyTimestamped: $trackIsFullyTimestamped")
                if (!trackIsFullyTimestamped) {
                    showSpeedPickerDialog()
                } else {
                    executeAsync(2.0F)
                }
            }
        }
    }

    fun getOkAction(): (Float) -> Unit = ::executeAsync

    private fun executeAsync(speedMperS: Float) {
        if (!isDataNonNull()) return
        val finalExportPojo = getFinalExportPOJO()
        onStartCallback()
        doAsync {
            val result = execute(finalExportPojo.file,
                    finalExportPojo.filename,
                    finalExportPojo.track,
                    speedMperS)
            uiThread {
                onFinishCallback(result)
            }
        }
    }

    fun getFinalExportPOJO(): ExportPOJO {
        val mostRecentFilename = editTextFilename.text.toString()
        val mostRecentFilenameNotEmptyAsserted = getFilename(mostRecentFilename, defaultFilename)
        return mergeExportPOJOS(exportPOJO,
                ExportPOJO(exportPOJO.file,
                        mostRecentFilenameNotEmptyAsserted,
                        exportPOJO.track))
    }

    private fun isDataNonNull(): Boolean {
        val track = exportPOJO.track
        track ?: return false
        val dir = exportPOJO.file
        if (dir == null) {
            callBackResultError("9 - trackPOJO.file = null")
            return false
        }
        if (!dir.exists() || !dir.isDirectory) {
            callBackResultError("10 - trackPOJO.file non existent or non directory")
            return false
        }
        if (track.points == null || track.points.size < 2) {
            callBackResultError("11 - trackpoints == null or start only")
            return false
        }
        if (track.stats == null) {
            callBackResultError("12 - stats == null")
            return false
        }
        if (!::editTextFilename.isInitialized) {
            callBackResultError("13 - editTextFilename - lateinit error")
            return false
        }
        if (!::defaultFilename.isInitialized) {
            callBackResultError("14 - defaultFilename - lateinit error")
            return false
        }
        return true
    }

    private fun callBackResultError(singleErrorMessage: String) {
        val debugMessage = listOf("debug:")
        val errorMessage = listOf("error:", singleErrorMessage)
        onFinishCallback(Result.Fail(debugMessage, errorMessage))
    }
}
