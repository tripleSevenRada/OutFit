package radim.outfit.core.export.logic

import android.util.Log

const val KEY_VALUE_FILENAME_TO_COURSENAME_DIV = "<DIVIDER_BETWEEN_FILENAME_AND_COURSENAME>"

class FilenameCoursenamePair{
    fun getEntry(keyValue: Pair<String, String>): String = "${keyValue.first}$KEY_VALUE_FILENAME_TO_COURSENAME_DIV${keyValue.second}"
    fun getPair(entry: String): Pair<String, String>{
        val list = entry.split(KEY_VALUE_FILENAME_TO_COURSENAME_DIV)
        if(list.size != 2) Log.e("getPair", "size != 2")
        return Pair(list[0], list[1])
    }
}