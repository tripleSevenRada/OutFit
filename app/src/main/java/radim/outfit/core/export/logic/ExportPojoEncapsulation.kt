package radim.outfit.core.export.logic

import android.content.SharedPreferences
import radim.outfit.core.export.work.locusapiextensions.track_preprocessing.TrackContainer
import java.io.File

data class ExportPOJO (val file: File?, val filename: String?, val trackContainer: TrackContainer?)

// merges non null values from the old POJO and the new one
fun mergeExportPOJOS(old: ExportPOJO, new: ExportPOJO): ExportPOJO{
    val file = new.file ?: old.file
    val filename = new.filename ?: old.filename
    val trackContainer = new.trackContainer ?: old.trackContainer
    return ExportPOJO(file,filename, trackContainer)
}

// EXPORT POJO UTILS

fun setRoot(root: File?, listener: ExportListener, prefs: SharedPreferences, key: String){
    listener.exportPOJO = mergeExportPOJOS(listener.exportPOJO, ExportPOJO(root, null, null))
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
    listener.exportPOJO = mergeExportPOJOS(listener.exportPOJO, ExportPOJO(null, null, trackContainer))
}

fun setFilename(filename: String, listener: ExportListener){
    listener.exportPOJO = mergeExportPOJOS(listener.exportPOJO, ExportPOJO(null, filename, null))
}