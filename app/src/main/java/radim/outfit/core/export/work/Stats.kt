package radim.outfit.core.export.work

import android.support.v7.app.AppCompatActivity
import locus.api.objects.extra.Track
import radim.outfit.getString

class Stats{

    fun basicInfo(track: Track, ctx: AppCompatActivity): String {
        val lengthInKm = (track.stats?.totalLength?: 0F) / 1000F
        val lengthInKmFormatted = String.format("%.3f", lengthInKm)
        return "${ctx.getString("name_stats")} ${track.name?:""}\n" +
                "${ctx.getString("length_stats")} $lengthInKmFormatted km\n" +
                "${ctx.getString("nmb_points_stats")} ${track.stats?.numOfPoints?:""}\n" +
                "${ctx.getString("nmb_waypoints_stats")} ${track.waypoints?.size?:""}"
    }
}