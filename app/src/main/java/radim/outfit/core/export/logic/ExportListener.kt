package radim.outfit.core.export.logic

import android.util.Log
import android.view.View
import android.widget.EditText
import locus.api.objects.extra.Track
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import radim.outfit.core.export.work.locusapiextensions.hasSpeed
import radim.outfit.core.export.work.locusapiextensions.isTimestamped
import radim.outfit.core.getFilename
import java.io.File


// error codes 8 - 13
class ExportListener(
        private val execute: (File?, String?, Track?, Float) -> Result,
        var exportPOJO: ExportPOJO,
        private val callback: (Result) -> Unit,
        private val clickedCallback: () -> Unit,
        private val showSpeedPickerDialog: ((Float) -> Unit) -> Unit
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

    private lateinit var finalExportPojo: ExportPOJO

    // syntax:
    // https://stackoverflow.com/questions/44912803/passing-and-using-function-as-constructor-argument-in-kotlin

    override fun onClick(v: View) {
        // SANITY CHECKS START
        val track = exportPOJO.track
        track ?: return
        //callBackResultError(" 8 - trackPOJO.track = null")
        val dir = exportPOJO.file
        if (dir == null) {
            callBackResultError("9 - trackPOJO.file = null")
            return
        }
        if (!dir.exists() || !dir.isDirectory) {
            callBackResultError("10 - trackPOJO.file non existent or non directory")
            return
        }
        if (track.points == null || track.points.size < 2) {
            callBackResultError("11 - trackpoints == null or start only")
            return
        }
        if (track.stats == null) {
            callBackResultError("12 - stats == null")
            return
        }
        // SANITY CHECKS END
        val mostRecentFilename = editTextFilename.text.toString()
        val mostRecentFilenameNotEmptyAsserted = getFilename(mostRecentFilename, defaultFilename)
        finalExportPojo = mergeExportPOJOS(exportPOJO,
                ExportPOJO(exportPOJO.file,
                        mostRecentFilenameNotEmptyAsserted,
                        exportPOJO.track))

        // https://medium.com/coding-blocks/making-asynctask-obsolete-with-kotlin-5fe1d944d69
        // https://antonioleiva.com/anko-background-kotlin-android/

        doAsync {
            val trackIsFullyTimestamped = track.isTimestamped()
            val trackHasSpeed = track.hasSpeed()
            uiThread {
                Log.i(tag, "trackIsFullyTimestamped: $trackIsFullyTimestamped")
                Log.i(tag, "trackHasSpeed, ONLY INFO: $trackHasSpeed")
                if (!trackIsFullyTimestamped) {
                    showSpeedPickerDialog(::executeAsync)
                } else {
                    executeAsync(2.0F)
                }
            }
        }
    }

    private fun executeAsync(speedMperS: Float) {
        if (::finalExportPojo.isInitialized) {
            clickedCallback()
            doAsync {
                val result = execute(finalExportPojo.file,
                        finalExportPojo.filename,
                        finalExportPojo.track,
                        speedMperS)
                uiThread {
                    callback(result)
                }
            }
        } else {
            callBackResultError("13 - lateinit finalExportPojo not initialized")
        }
    }

    fun getOkAction(): (Float) -> Unit = ::executeAsync

    private fun callBackResultError(singleErrorMessage: String) {
        val debugMessage = listOf("debug:")
        val errorMessage = listOf("error:", singleErrorMessage)
        callback(Result.Fail(debugMessage, errorMessage))
    }

}
