package radim.outfit.core.export.work

import android.util.Log
import locus.api.objects.extra.Track

val tag = "CP"

fun printCoursepoints(track: Track){
    track.waypoints.forEach{
        Log.i(tag, "----------------------------------")
        Log.i(tag, it.name)
        if(it.hasParameterDescription()){
            Log.i(tag, it.parameterDescription)
        }
        Log.i(tag, it.parameterRteAction.toString())
    }
}

