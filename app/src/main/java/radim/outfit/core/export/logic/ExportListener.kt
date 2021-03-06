package radim.outfit.core.export.logic

import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import radim.outfit.core.export.work.getFilename
import radim.outfit.core.export.work.locusapiextensions.track_preprocessing.TrackContainer
import radim.outfit.core.export.work.locusapiextensions.isTimestamped
import radim.outfit.core.viewmodels.MainActivityViewModel
import radim.outfit.getString
import java.io.File

interface PermInfoProvider {
    fun permWriteIsGranted(): Boolean
}

interface Toaster {
    fun toast(key: String, length: Int)
}

// error codes 8 - 15
// https://drive.google.com/file/d/1wwYzoPQts1HreDpS614oMAVyafU07ZYF/view?usp=sharing

class ExportListener(
        private val execute: (File?, String?, TrackContainer?, Float, AppCompatActivity, MutableList<String>) -> Result,
        var exportPOJO: ExportPOJO,
        private val onFinishCallback: (Result, MainActivityViewModel) -> Unit,
        private val onStartCallback: () -> Unit,
        private val showSpeedPickerDialog: () -> Unit,
        private val ctx: AppCompatActivity,
        private val permInfoProvider: PermInfoProvider,
        private val toaster: Toaster,
        private val viewModel: MainActivityViewModel,
        private val debugMessages: MutableList<String>
) : View.OnClickListener {

    private val tag = "ExportListener"

    private lateinit var editTextFilename: EditText
    private lateinit var defaultFilename: String

    @Throws(java.lang.RuntimeException::class)
    fun attachView(v: View?) {
        if (v is EditText) editTextFilename = v else throw RuntimeException("EditTextOnly")
    }

    fun attachDefaultFilename(filename: String) {
        this.defaultFilename = filename
    }

    override fun onClick(v: View?) {
        if (!permInfoProvider.permWriteIsGranted()) {
            toaster.toast(ctx.getString("permission_needed"), Toast.LENGTH_LONG)
            return
        }
        if (!dataIsValid()) return
        val trackContainer = exportPOJO.trackContainer
        trackContainer ?: return

        // https://medium.com/coding-blocks/making-asynctask-obsolete-with-kotlin-5fe1d944d69
        // https://antonioleiva.com/anko-background-kotlin-android/

        doAsync {
            val trackIsFullyTimestamped = trackContainer.track.isTimestamped()
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

    // SpeedPickerDialog (Fragment)
    fun getOkAction(): (Float) -> Unit = ::executeAsync

    private fun executeAsync(speedMperS: Float) {
        if (!dataIsValid()) return
        val finalExportPojo = getFinalExportPOJO()
        onStartCallback()
        viewModel.exportInProgress = true
        doAsync {
            val result = execute(finalExportPojo.file,
                    finalExportPojo.filename,
                    finalExportPojo.trackContainer,
                    speedMperS,
                    ctx,
                    debugMessages
            )
            uiThread {
                viewModel.exportInProgress = false
                onFinishCallback(result, viewModel)
            }
        }
    }

    private fun getFinalExportPOJO(): ExportPOJO {
        val mostRecentFilename = editTextFilename.text.toString()
        val mostRecentFilenameNotEmptyAsserted = getFilename(mostRecentFilename, defaultFilename)
        return mergeExportPOJOS(exportPOJO,
                ExportPOJO(exportPOJO.file,
                        mostRecentFilenameNotEmptyAsserted,
                        exportPOJO.trackContainer))
    }

    private fun dataIsValid(): Boolean {
        val trackContainer = exportPOJO.trackContainer
        trackContainer ?: return false
        val dir = exportPOJO.file
        if (dir == null) {
            callBackResultError("9 - trackPOJO.file = null")
            return false
        }
        if (try {
                    !dir.isDirectory
                } catch (e: Exception) {
                    false
                }) {
            callBackResultError("10 - trackPOJO.file non existent or non directory")
            return false
        }
        if (trackContainer.track.points == null || trackContainer.track.points.size < 2) {
            callBackResultError("11 - trackpoints == null or start only")
            return false
        }
        if (trackContainer.track.stats == null) {
            callBackResultError("12 - stats == null")
            return false
        }
        if (trackContainer.track.stats.totalLength < 1.0) {
            callBackResultError("13 - totalLength < 1.0")
            return false
        }
        if (!::editTextFilename.isInitialized) {
            callBackResultError("14 - editTextFilename - lateinit error")
            return false
        }
        if (!::defaultFilename.isInitialized) {
            callBackResultError("15 - defaultFilename - lateinit error")
            return false
        }
        return true
    }

    private fun callBackResultError(singleErrorMessage: String) {
        val debugMessage = listOf("debug:")
        val errorMessage = listOf("error:", singleErrorMessage)
        onFinishCallback(Result.Fail(debugMessage, errorMessage), viewModel)
    }
}
