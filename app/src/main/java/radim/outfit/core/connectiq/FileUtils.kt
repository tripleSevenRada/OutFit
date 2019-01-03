package radim.outfit.core.connectiq

import android.util.Log
import java.io.File

const val LOG_TAG_F_UTILS = "FILE_UTILS"

fun getListOfFitFilesRecursively(dir: File): List<File> {
    val files = mutableListOf<File>()
    if (dir.isDirectory) {
        dir.walk().forEach {
            if (it.isFile && it.name.endsWith(".fit", false))
                files.add(it)
        }
    } else {
        Log.w(LOG_TAG_F_UTILS, "dir is NOT directory")
    }
    return files
}