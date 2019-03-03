package radim.outfit.core.export.logic

import android.support.v7.app.AppCompatActivity
import locus.api.objects.extra.Track
import radim.outfit.core.export.work.Encoder
import java.io.File

class ExportFunction: (File?, String?, Track?, Float, AppCompatActivity, MutableList<String>)-> Result{

    override operator fun invoke(dir: File?, filename: String?, track: Track?,
                                 speedIfNotInTrack: Float, ctx: AppCompatActivity, debugMessages: MutableList<String>): Result{
        return if(dir != null && filename != null && track != null){
            Encoder(debugMessages).encode(track, dir, filename, speedIfNotInTrack, ctx)
        } else {
            Result.Fail(listOf("debug:"), listOf("error:", "dir or filename or track == null"), dir, filename)
        }
    }
}