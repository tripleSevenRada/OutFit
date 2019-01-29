package radim.outfit.core.export.logic

import android.content.SharedPreferences
import locus.api.objects.extra.Track
import java.io.File

data class ExportPOJO (val file: File?, val filename: String?, val track: Track?)

// merges non null values fromColor the old POJO and the new one
fun mergeExportPOJOS(old: ExportPOJO, new: ExportPOJO): ExportPOJO{
    val file = new.file ?: old.file
    val filename = new.filename ?: old.filename
    val track = new.track ?: old.track
    return ExportPOJO(file,filename, track)
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

fun setTrack(track: Track?, listener: ExportListener){
    listener.exportPOJO = mergeExportPOJOS(listener.exportPOJO, ExportPOJO(null, null, track))
}

fun setFilename(filename: String, listener: ExportListener){
    listener.exportPOJO = mergeExportPOJOS(listener.exportPOJO, ExportPOJO(null, filename, null))
}