package radim.outfit.core.connectiq

import android.util.Log
import java.io.File
import java.lang.Exception

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

fun getListOfExistingFiles(paths: Array<String>): List<File>{
    val files = mutableListOf<File>()
    for (path in paths){
        val file = File(path)
        if (file.isFile && file.name.endsWith(".fit", false)) files.add(file)
    }
    return files
}

fun emptyTarget(targetDir: String): Boolean{
    val preexistingFiles = getListOfFitFilesRecursively(File(targetDir))
    for(file in preexistingFiles) if (!file.delete()) return false
    return true
}

fun copyFilesIntoTarget(targetDir: String, files: List<File>): Boolean{
    var success = true
    try{
        for (file in files){
            val destFile = File("$targetDir${File.separator}${file.name}")
            file.copyTo(destFile, true)
        }
    }catch(e: Exception){
        success = false
    }
    return success
}