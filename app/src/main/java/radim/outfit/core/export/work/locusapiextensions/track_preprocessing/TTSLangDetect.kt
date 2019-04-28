package radim.outfit.core.export.work.locusapiextensions.track_preprocessing

import android.util.Log
import locus.api.objects.extra.Point

class TTSLangDetect{

    enum class TTSLang{
        EN, CS, UNKNOWN
    }

    val TTSStringsEN: Set<String> = setOf(
            "undefined",
            " no_maneuver",
            "straight",
            "name_change",
            "left_slight",
            "left",
            "left_sharp",
            "right_slight",
            "right",
            "right_sharp",
            "stay_left",
            "stay_right",
            "stay_straight",
            "u-turn",
            "u-turn_left",
            "u-turn_right",
            "exit_left",
            "exit_right",
            "ramp_left",
            "ramp_right",
            "ramp_straight",
            "merge_left",
            "merge_right",
            "merge",
            "enter_state",
            "dest",
            "dest_left",
            "dest_right",
            "roundabout_e1",
            "roundabout_e2",
            "roundabout_e3",
            "roundabout_e4",
            "roundabout_e5",
            "roundabout_e6",
            "roundabout_e7",
            "roundabout_e8",
            "pass_place"
    )

    fun detectLang(points: Iterable<Point>): TTSLang{
        var count = 0
        points.forEach {
            Log.e("EE","-----------------------------------")
            Log.e("EE",it.name + count)
            Log.e("EE",it.parameterRteAction.name + count)
            Log.e("EE",it.parameterRteAction.textId + count)
            Log.e("EE",it.parameterRteAction.toString())
            count ++
        }
        return if( points.all { TTSStringsEN.contains(it.parameterRteAction.name) }) TTSLang.EN
        else TTSLang.UNKNOWN
    }

}