package radim.outfit.core.export.logic

import locus.api.objects.extra.Track
import java.io.File

class ExportFunction: (File?, String?, Track?)-> ResultPOJO{

    override operator fun invoke(file: File?, filename: String?, track: Track?): ResultPOJO{
        // TODO real work finally
        val result = ResultPOJO(mutableListOf(),mutableListOf(),mutableListOf())
        result.addToPublicMessage("performed with: File: $file, Filename: $filename, Track: $track")
        result.addToDebugMessage("no debug")
        result.addToErrorMessage("no error")
        return result
    }

}