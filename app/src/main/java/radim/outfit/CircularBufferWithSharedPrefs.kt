package radim.outfit

import android.content.SharedPreferences

const val CIRC_BUFF_SIZE = 7 // be careful, call init if changed
const val CIRC_BUFF_KEY_PREFIX = "buffer_index_"
const val CIRC_BUFF_POINTER_KEY = "buffer_pointer"

// keep track of last CIRC_BUFF_SIZE exports

fun initCircularBuffer(prefsEdit: SharedPreferences.Editor) {
    with(prefsEdit) {
        // key: String, value: String
        for (i in 0 until CIRC_BUFF_SIZE) {
            putString("$CIRC_BUFF_KEY_PREFIX$i", "")
        }
        putInt(CIRC_BUFF_POINTER_KEY, 0)
        // apply() calls caller of this method
    }
}

fun writeToCircularBuffer(value: String, prefs: SharedPreferences) {
    val pointer = prefs.getInt(CIRC_BUFF_POINTER_KEY, 0)
    val movedPointer = if (pointer == (CIRC_BUFF_SIZE - 1)) 0
    else pointer + 1
    with(prefs.edit()) {
        putString("$CIRC_BUFF_KEY_PREFIX$pointer", value)
        putInt(CIRC_BUFF_POINTER_KEY, movedPointer)
        apply()
    }
}

fun readCircularBuffer(prefs: SharedPreferences): Array<String> {
    val buffer = Array(CIRC_BUFF_SIZE) { _ -> "" }
    val pointer = prefs.getInt(CIRC_BUFF_POINTER_KEY, 0)
    var bufferIndex = 0
    // read left side of the buffer
    for (i in (pointer - 1) downTo 0) {
        buffer[bufferIndex] = prefs.getString("$CIRC_BUFF_KEY_PREFIX$i", "") ?: ""
        bufferIndex++
    }
    for (i in (CIRC_BUFF_SIZE - 1) downTo pointer) {
        buffer[bufferIndex] = prefs.getString("$CIRC_BUFF_KEY_PREFIX$i", "") ?: ""
        bufferIndex++
    }


    return buffer
}
