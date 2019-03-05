package radim.outfit.core.share.work

import android.content.SharedPreferences

const val CIRC_BUFF_SIZE = 7 // be careful, call init if changed

// per instance used to concat with id
const val CIRC_BUFF_KEY_PREFIX = "buffer_index_"
const val CIRC_BUFF_POINTER_KEY_PREFIX = "buffer_pointer_"

// keep trackContainer of last CIRC_BUFF_SIZE exports

class CircularBufferWithSharedPrefs(val id: Int) {

    fun initCircularBuffer(prefsEdit: SharedPreferences.Editor) {
        with(prefsEdit) {
            // key: String, value: String
            for (i in 0 until CIRC_BUFF_SIZE) {
                putString("$CIRC_BUFF_KEY_PREFIX$id-$i", "")
            }
            putInt("$CIRC_BUFF_POINTER_KEY_PREFIX$id", 0)
            // caller of this method calls apply()
        }
    }

    fun writeToCircularBuffer(value: String, prefs: SharedPreferences) {
        val pointer = prefs.getInt("$CIRC_BUFF_POINTER_KEY_PREFIX$id", 0)
        val movedPointer = if (pointer == (CIRC_BUFF_SIZE - 1)) 0
        else pointer + 1
        val prevPointer = if (pointer > 0) pointer - 1
        else (CIRC_BUFF_SIZE - 1)
        val prevValue = prefs.getString("$CIRC_BUFF_KEY_PREFIX$id-$prevPointer", "")
        if (value == prevValue) return
        with(prefs.edit()) {
            putString("$CIRC_BUFF_KEY_PREFIX$id-$pointer", value)
            putInt("$CIRC_BUFF_POINTER_KEY_PREFIX$id", movedPointer)
            apply()
            // apply() commits its changes to the in-memory SharedPreferences
            // immediately but starts an asynchronous commit to disk and
            // you won't be notified of any failures.
        }
    }

    fun readCircularBuffer(prefs: SharedPreferences): Array<String> {
        val buffer = Array(CIRC_BUFF_SIZE) { "" }
        val pointer = prefs.getInt("$CIRC_BUFF_POINTER_KEY_PREFIX$id", 0)
        var bufferIndex = 0
        // read left side of the buffer
        for (i in (pointer - 1) downTo 0) {
            buffer[bufferIndex] = prefs.getString("$CIRC_BUFF_KEY_PREFIX$id-$i", "") ?: ""
            bufferIndex++
        }
        for (i in (CIRC_BUFF_SIZE - 1) downTo pointer) {
            buffer[bufferIndex] = prefs.getString("$CIRC_BUFF_KEY_PREFIX$id-$i", "") ?: ""
            bufferIndex++
        }
        return buffer
    }
}

fun Array<String>.ridDuplicities(): Array<String> {
    val bufferSet = mutableSetOf<String>()
    val noDuplicities = mutableListOf<String>()
    this.forEach {
        if (!bufferSet.contains(it)) {
            bufferSet.add(it)
            noDuplicities.add(it)
        }
    }
    return noDuplicities.toTypedArray()
}

fun Array<String>.ridEmpty(): Array<String> = this.filter { !it.isEmpty() }.toTypedArray()
