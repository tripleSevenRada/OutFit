package radim.outfit.core.export.logic

import android.view.View
import locus.api.objects.extra.Track
import java.io.File

class ExportListener(
        private val execute: (File?, String?, Track?)-> ResultPOJO,
        var exportPOJO: ExportPOJO,
        private val callback: (ResultPOJO) -> Unit
        ): View.OnClickListener{

    // syntax:
    // https://stackoverflow.com/questions/44912803/passing-and-using-function-as-constructor-argument-in-kotlin

    override fun onClick(v: View){
        callback(execute(exportPOJO.file, exportPOJO.filename, exportPOJO.track))
    }

}