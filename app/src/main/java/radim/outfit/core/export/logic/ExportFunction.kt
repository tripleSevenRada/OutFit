package radim.outfit.core.export.logic

import android.support.v7.app.AppCompatActivity
import radim.outfit.R
import radim.outfit.core.export.work.Encoder
import radim.outfit.core.export.work.locusapiextensions.track_preprocessing.*
import java.io.File

class ExportFunction : (File?, String?, TrackContainer?, Float, AppCompatActivity, MutableList<String>) -> Result {

    override operator fun invoke(dir: File?, filename: String?, trackContainer: TrackContainer?,
                                 speedIfNotInTrack: Float, ctx: AppCompatActivity, debugMessages: MutableList<String>): Result {
        return if (dir != null && filename != null && trackContainer != null) {

            // WaypointFilter
            // FilterAlwaysDismissible extends WaypointFilter
            // Simplify extends WaypointFilter

            var container = trackContainer

            // NOT OPTIONAL
            FilterAlwaysDismissible(container).filter()

            // TODO HARDCODED!

            // clusterize
            // OPTIONAL
            // Kruskal(debugMessages).clusterize(60.0, container, ctx.getString(R.string.moreActions))
            // filter dismissible
            // OPTIONAL
            // Simplify(container).simplify()
            // move towards start moveDist metres
            // OPTIONAL
            container = Move(debugMessages).move(40.0, container) // returns new container

            // export work
            // NOT OPTIONAL
            Encoder(debugMessages).encode(container, dir, filename, speedIfNotInTrack, ctx)

        } else {
            Result.Fail(listOf("debug:"), listOf("error:", "dir or filename or trackContainer == null"), dir, filename)
        }
    }
}