package radim.outfit

import org.junit.Test
import org.junit.Assert.*
import radim.outfit.core.export.logic.FilenameCoursenamePair

class FileNameCourseNameUnitTest{

    @Test
    fun rudimentary(){
        val key = "myKey"
        val value = "myValue"
        val lab = FilenameCoursenamePair()
        val entry = lab.getEntry(Pair(key, value))
        val pair = lab.getPair(entry)
        assertEquals(key, pair.first)
        assertEquals(value, pair.second)
    }

}