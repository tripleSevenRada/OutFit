package radim.outfit.core.export.logic

import locus.api.objects.extra.Track
import java.io.File

data class ExportPOJO (val file: File?, val filename: String?, val track: Track?)

// merges non null values from the old POJO and the new one
fun mergeExportPOJOS(old: ExportPOJO, new: ExportPOJO): ExportPOJO{
    val file = new.file ?: old.file
    val filename = new.filename ?: old.filename
    val track = new.track ?: old.track
    return ExportPOJO(file,filename, track)
}

// EXPORT POJO UTILS

fun setRoot(root: File?, listener: ExportListener){
    val exportPOJOold = listener.exportPOJO
    val exportPOJOnew = mergeExportPOJOS(exportPOJOold, ExportPOJO(root, null, null))
    listener.exportPOJO = exportPOJOnew
}

fun getRoot(listener: ExportListener): File?{
    return listener.exportPOJO.file
}