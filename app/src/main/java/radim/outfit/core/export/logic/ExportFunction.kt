package radim.outfit.core.export.logic

import android.util.Log
import locus.api.objects.extra.Track
import radim.outfit.core.export.work.Encoder
import java.io.File

class ExportFunction: (File?, String?, Track?)-> Result{

    override operator fun invoke(dir: File?, filename: String?, track: Track?): Result{
        return if(dir != null && filename != null && track != null){
            Encoder().encode(track, dir, filename)
        } else {
            Result.Fail(listOf("debug:"), listOf("error:", "dir or filename or track == null"), dir, filename)
        }
    }
}