package radim.outfit.core.export.logic

import android.content.SharedPreferences
import locus.api.objects.extra.Location
import locus.api.objects.extra.Point
import radim.outfit.core.export.work.locusapiextensions.track_preprocessing.TrackContainer
import java.io.File

data class ExportPOJO (val file: File?,
                       val filename: String?,
                       val trackContainer: TrackContainer?,
                       val originalPoints: List<Location>?)

// merges non null values from the old POJO and the new one
fun mergeExportPOJOS(old: ExportPOJO, new: ExportPOJO): ExportPOJO{
    val file = new.file ?: old.file
    val filename = new.filename ?: old.filename
    val trackContainer = new.trackContainer ?: old.trackContainer
    val originalPoints = new.originalPoints ?: old.originalPoints
    return ExportPOJO(file,filename, trackContainer, originalPoints)
}

// EXPORT POJO UTILS

fun setRoot(root: File?, listener: ExportListener, prefs: SharedPreferences, key: String){
    listener.exportPOJO = mergeExportPOJOS(listener.exportPOJO, ExportPOJO(root, null, null, null))
    if(root != null) {
        with(prefs.edit()) {
            putString(key, root.absolutePath)
            apply()
        }
    }
}

fun getRoot(listener: ExportListener): File?{
    return listener.exportPOJO.file
}

fun setTrack(trackContainer: TrackContainer?, listener: ExportListener){
    listener.exportPOJO = mergeExportPOJOS(listener.exportPOJO, ExportPOJO(null, null, trackContainer, null))
}

fun setFilename(filename: String, listener: ExportListener){
    listener.exportPOJO = mergeExportPOJOS(listener.exportPOJO, ExportPOJO(null, filename, null, null))
}

fun setOriginalPoints(trackContainer: TrackContainer?, listener: ExportListener){
    val originalPoints = mutableListOf<Location>()
    trackContainer?.track?.points?.forEach {
        if(it != null) {
            val newLocation = Location()
            newLocation.setLatitude(it.latitude)
            newLocation.setLongitude(it.longitude)
            newLocation.altitude = it.altitude
            newLocation.speed = it.speed
            newLocation.time = it.time
            originalPoints.add(newLocation)
        }
    }
    listener.exportPOJO = mergeExportPOJOS(listener.exportPOJO, ExportPOJO(null, null, null, originalPoints))
}