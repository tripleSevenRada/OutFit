package radim.outfit.core

import android.text.InputFilter
import locus.api.objects.extra.Track
import android.text.Spanned
import android.util.Log

class Filename{

    fun getFilename(track: Track): String {
        return FilenameCharsFilter().filterSimple(track.name).toString()
    }

}

const val RESERVED_CHARS = "?:\"*|/\\<>"

class FilenameCharsFilter: InputFilter {

    override fun filter(source: CharSequence, start: Int, end: Int,
               dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        // TODO rewrite
        Log.i("FILTER", "in $source")
        Log.i("FILTER", "start $start")
        Log.i("FILTER", "end $end")
        if (source.isEmpty()) return null
        val filtered = source.filter { RESERVED_CHARS.indexOf(it) == -1 }
        Log.i("FILTER", "out $filtered")
        return filtered
    }

    fun filterSimple(source: CharSequence): CharSequence{
        return source.filter { RESERVED_CHARS.indexOf(it) == -1 }
    }
}