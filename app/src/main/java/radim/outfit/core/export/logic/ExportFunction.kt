package radim.outfit.core.export.logic

import android.support.v7.app.AppCompatActivity
import radim.outfit.core.export.work.Encoder
import radim.outfit.core.export.work.locusapiextensions.track_preprocessing.TrackContainer
import java.io.File

class ExportFunction : (File?, String?, TrackContainer?, Float, AppCompatActivity, MutableList<String>) -> Result {

    override operator fun invoke(dir: File?, filename: String?, trackContainer: TrackContainer?,
                                 speedIfNotInTrack: Float, ctx: AppCompatActivity, debugMessages: MutableList<String>): Result {
        return if (dir != null && filename != null && trackContainer != null) {
            Encoder(debugMessages).encode(trackContainer, dir, filename, speedIfNotInTrack, ctx)
        } else {
            Result.Fail(listOf("debug:"), listOf("error:", "dir or filename or trackContainer == null"), dir, filename)
        }
    }
}