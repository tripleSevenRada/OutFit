package radim.outfit.core.export.work

import android.text.InputFilter
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils

fun getFilename(fileName: String?, defaultFilename: String): String {
    if(fileName == null)return "$defaultFilename$FILENAME_SUFFIX" // should never happen, Locus
    val filename = FilenameCharsFilter().replaceReservedChars(fileName,
            FILENAME_RESERVED_CHARS,
            FILENAME_REPLACEMENT_CHAR).toString()
    return if (filename.isNotEmpty()) {
        if(!filename.endsWith(FILENAME_SUFFIX)) {
            if(filename.length > 14) "${filename.subSequence(0, 14)}$FILENAME_SUFFIX"
            else "$filename$FILENAME_SUFFIX"
        }
        else {
            val stripped = filename.subSequence(0, fileName.length - 4)
            if(stripped.length > 14) "${stripped.subSequence(0, 14)}$FILENAME_SUFFIX"
            else "$stripped$FILENAME_SUFFIX"
        }
    } else {
        "$defaultFilename$FILENAME_SUFFIX"
    }
}

//https://www.url-encode-decode.com/
const val FILENAME_SUFFIX = ".fit"
const val FILENAME_REPLACEMENT_CHAR = '-'
val FILENAME_RESERVED_CHARS = setOf(' ', '#', '%', '&', '{', '}', '$', '@', ':', '?', '!', '\"', '\'', '*', '|', '/', '\\', '<', '>')

class FilenameCharsFilter : InputFilter {
    /*
    This method is called when the buffer is isInProgress to replace the range dstart … dend
    of dest with the new text from the range start … end of source. Return the CharSequence
    that you would like to have placed there instead, including an empty string if appropriate,
    or null to accept the original replacement. Be careful to not to reject 0-length replacements,
    as this is what happens when you delete text. Also beware that you should not attempt to make
    any changes to dest from this method; you may only examine it for context. Note: If source
    is an instance of Spanned or Spannable, the span objects in the source should be copied into
    the filtered result (i.e. the non-null return value).
    TextUtils.copySpansFrom(Spanned, int, int, Class, Spannable, int) can be used for convenience
    if the span boundary indices would be remaining identical relative to the source.
    */
    override fun filter(source: CharSequence, start: Int, end: Int,
                        dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        for (i in start until end) {
            if (FILENAME_RESERVED_CHARS.contains(source[i])) {
                val replaced = replaceReservedChars(source.subSequence(start, end),
                        FILENAME_RESERVED_CHARS, FILENAME_REPLACEMENT_CHAR)
                return if (source is Spanned) {
                    val spannableStringReplaced = SpannableString(replaced)
                    TextUtils.copySpansFrom(source,
                            start, end, null, spannableStringReplaced, 0)
                    spannableStringReplaced
                } else {
                    replaced
                }
            }
        }
        return null // keep original
    }

    fun replaceReservedChars(source: CharSequence, reserved: Set<Char>,
                             replacementChar: Char): CharSequence {
        val sb = StringBuilder()
        source.forEach {
            if (reserved.contains(it)) sb.append(replacementChar)
            else sb.append(it)
        }
        return sb.subSequence(0, sb.length)
    }

    private fun ridReservedChars(source: CharSequence, reserved: Set<Char>): CharSequence {
        return source.filter { !reserved.contains(it) }
    }
}