package radim.outfit.core.export.logic

import android.view.View
import android.widget.EditText
import locus.api.objects.extra.Track
import radim.outfit.core.getFilename
import java.io.File

class ExportListener(
        private val execute: (File?, String?, Track?)-> ResultPOJO,
        var exportPOJO: ExportPOJO,
        private val callback: (ResultPOJO) -> Unit
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

        if(exportPOJO.track == null) {
            callBackResultError("trackPOJO.track = null")
            return
        }
        if(exportPOJO.file == null) {
            callBackResultError("trackPOJO.file = null")
            return
        }
        val dir = exportPOJO.file
        if(dir != null && (!dir.exists() || !dir.isDirectory )){
            callBackResultError("trackPOJO.file non existent or non directory")
            return
        }

        val mostRecentFilename = editTextFilename.text.toString()
        val mostRecentFilenameNotEmptyAsserted = getFilename(mostRecentFilename, defaultFilename)
        val finalExportPojo = mergeExportPOJOS(exportPOJO,
                ExportPOJO(exportPOJO.file,
                mostRecentFilenameNotEmptyAsserted,
                        exportPOJO.track))

        callback(execute(finalExportPojo.file, finalExportPojo.filename,finalExportPojo.track))
    }

    private fun callBackResultError(errorMessage: String){
        val result = ResultPOJO(mutableListOf(), mutableListOf(), mutableListOf())
        result.addToErrorMessage(errorMessage)
        callback(result)
    }

}