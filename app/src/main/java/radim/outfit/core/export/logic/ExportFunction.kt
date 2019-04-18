package radim.outfit.core.export.logic

import android.support.v7.app.AppCompatActivity
import android.util.Log
import radim.outfit.DEBUG_MODE
import radim.outfit.ExportOptionsDataProvider
import radim.outfit.R
import radim.outfit.core.export.work.Encoder
import radim.outfit.core.export.work.locusapiextensions.track_preprocessing.*
import java.io.File

class ExportFunction : (File?, String?, TrackContainer?, Float, AppCompatActivity, MutableList<String>) -> Result {

    override operator fun invoke(dir: File?, filename: String?, trackContainer: TrackContainer?,
                                 speedIfNotInTrack: Float, ctx: AppCompatActivity, debugMessages: MutableList<String>): Result {
        return if (dir != null && filename != null && trackContainer != null) {

            val minDistWP = 50.0

            var container = trackContainer
            val exportOptionsDataProvider = ctx as ExportOptionsDataProvider

            container.clusterize = exportOptionsDataProvider.getBundle()
            container.move = exportOptionsDataProvider.getMove()
            container.moveDist = try {
                Integer.valueOf(exportOptionsDataProvider.getMoveDist()).toDouble()
            }catch (e: Exception){
                10.0
            }

            if(DEBUG_MODE){
                Log.i("EXPORT FUNCTION", "container.clusterize: ${container.clusterize}")
                Log.i("EXPORT FUNCTION", "container.move: ${container.move}")
                Log.i("EXPORT FUNCTION", "container.moveDist: ${container.moveDist}")
            }

            // WaypointFilter
            // FilterAlwaysDismissible extends WaypointFilter
            // Simplify extends WaypointFilter

            // NOT OPTIONAL
            FilterAlwaysDismissible(container).filterAlwaysDismissible()

            // clusterize
            // OPTIONAL
            if (container.clusterize) Kruskal(debugMessages).clusterize(minDistWP, container, ctx.getString(R.string.moreActions))
            // filter dismissible
            // NOT OPTIONAL
            Simplify(container).simplify()
            // move towards start moveDist metres
            // OPTIONAL
            if (container.move) container = Move(debugMessages).move(container.moveDist, container) // returns new container

            // export work
            // NOT OPTIONAL
            Encoder(debugMessages).encode(container, dir, filename, speedIfNotInTrack, ctx)

        } else {
            Result.Fail(listOf("debug:"), listOf("error:", "dir or filename or trackContainer == null"), dir, filename)
        }
    }
}