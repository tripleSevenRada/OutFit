package radim.outfit.core.export.logic

import locus.api.objects.extra.Track
import radim.outfit.core.export.work.Encoder
import java.io.File

class ExportFunction: (File?, String?, Track?, Float)-> Result{

    override operator fun invoke(dir: File?, filename: String?, track: Track?,
                                 speedIfNotInTrack: Float): Result{
        return if(dir != null && filename != null && track != null){
            Encoder().encode(track, dir, filename, speedIfNotInTrack)
        } else {
            Result.Fail(listOf("debug:"), listOf("error:", "dir or filename or track == null"), dir, filename)
        }
    }
}