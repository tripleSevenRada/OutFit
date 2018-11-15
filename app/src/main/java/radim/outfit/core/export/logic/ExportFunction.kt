package radim.outfit.core.export.logic

import locus.api.objects.extra.Track
import radim.outfit.core.export.work.Encoder
import java.io.File

class ExportFunction: (File?, String?, Track?)-> ResultPOJO{

    override operator fun invoke(file: File?, filename: String?, track: Track?): ResultPOJO{
        return if(file != null && filename != null && track != null){
            Encoder().encode(track, file, filename)
        } else {
            val result = ResultPOJO(mutableListOf(),mutableListOf(),mutableListOf())
            result.addToPublicMessage("Dir: $file, Filename: $filename, Track: $track")
            result.addToDebugMessage("null value, should never happen here")
            result.addToErrorMessage("null value")
            result
        }
    }
}