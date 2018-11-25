package radim.outfit.debugdumps

import java.io.File
import java.io.IOException

@Throws(IOException::class)
fun writeTextFile(file: File, message: List<String>) {
    file.bufferedWriter(Charsets.UTF_8, 2048).use {
        val listIt = message.iterator()
        while (listIt.hasNext()) {
            it.append(listIt.next() + "\n")
        }
    }
}