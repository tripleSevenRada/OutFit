package radim.outfit.core.export.logic

import android.view.View
import android.widget.EditText
import locus.api.objects.extra.Track
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import radim.outfit.core.getFilename
import java.io.File


// error codes 8 - 12
class ExportListener(
        private val execute: (File?, String?, Track?)-> Result,
        var exportPOJO: ExportPOJO,
        private val callback: (Result) -> Unit,
        private val clickedCallback: () -> Unit
        ): View.OnClickListener{

    private lateinit var editTextFilename: EditText
    private lateinit var defaultFilename: String

    @Throws(java.lang.RuntimeException::class)
    fun attachView(v: View){
        if (v is EditText) editTextFilename = v else throw RuntimeException("EditTextOnly")
    }
    fun attachDefaultFilename(filename: String){
        this.defaultFilename = filename
    }
    // syntax:
    // https://stackoverflow.com/questions/44912803/passing-and-using-function-as-constructor-argument-in-kotlin

    override fun onClick(v: View){
        // SANITY CHECKS START
        val track = exportPOJO.track
        if(track == null) {
            callBackResultError(" 8 - trackPOJO.track = null")
            return
        }
        val dir = exportPOJO.file
        if(dir == null) {
            callBackResultError("9 - trackPOJO.file = null")
            return
        }
        if(!dir.exists() || !dir.isDirectory ){
            callBackResultError("10 - trackPOJO.file non existent or non directory")
            return
        }
        if(track.points == null || track.points.size < 2){
            callBackResultError("11 - trackpoints == null or start only")
            return
        }
        if(track.stats == null){
            callBackResultError("12 - stats == null")
            return
        }
        // SANITY CHECKS END
        val mostRecentFilename = editTextFilename.text.toString()
        val mostRecentFilenameNotEmptyAsserted = getFilename(mostRecentFilename, defaultFilename)
        val finalExportPojo = mergeExportPOJOS(exportPOJO,
                ExportPOJO(exportPOJO.file,
                mostRecentFilenameNotEmptyAsserted,
                        exportPOJO.track))

        // https://medium.com/coding-blocks/making-asynctask-obsolete-with-kotlin-5fe1d944d69
        // https://antonioleiva.com/anko-background-kotlin-android/

        clickedCallback()

        doAsync {
            val result = execute(finalExportPojo.file, finalExportPojo.filename, finalExportPojo.track)
            uiThread {
                callback(result)
            }
        }
    }

    private fun callBackResultError(singleErrorMessage: String){
        val debugMessage = listOf("debug:")
        val errorMessage = listOf("error:", singleErrorMessage)
        callback(Result.Fail(debugMessage, errorMessage))
    }

}
