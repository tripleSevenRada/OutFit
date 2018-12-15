package radim.outfit.core.export.logic

import android.support.v7.app.AppCompatActivity
import locus.api.objects.extra.Track
import radim.outfit.core.export.work.Encoder
import java.io.File

class ExportFunction: (File?, String?, Track?, Float, AppCompatActivity)-> Result{

    override operator fun invoke(dir: File?, filename: String?, track: Track?,
                                 speedIfNotInTrack: Float, ctx: AppCompatActivity): Result{
        return if(dir != null && filename != null && track != null){
            Encoder().encode(track, dir, filename, speedIfNotInTrack, ctx)
        } else {
            Result.Fail(listOf("debug:"), listOf("error:", "dir or filename or track == null"), dir, filename)
        }
    }
}