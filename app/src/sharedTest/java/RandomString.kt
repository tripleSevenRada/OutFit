package radim.outfit

import kotlin.streams.asSequence


fun getRandomString(length: Long): String{
    val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    return java.util.Random().ints(length, 0, source.length)
            .asSequence()
            .map(source::get)
            .joinToString("")
}