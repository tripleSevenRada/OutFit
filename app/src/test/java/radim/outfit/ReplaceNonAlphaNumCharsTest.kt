package radim.outfit

import org.junit.Assert.assertEquals
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import radim.outfit.core.export.work.replaceNonAllowedChars

private val log: Logger = LoggerFactory.getLogger(ReplaceNonAlphaNumCharsTest::class.java)

class ReplaceNonAlphaNumCharsTest{
    @Test
    fun testTest(){
        assertEquals("yes","yes")
        log.info("ReplaceNonAlphaNumCharsTest")
    }
    @Test
    fun printASCIICharsAndReplacementResult(){
        for (i in 32..126){
            val char: Char = i.toChar()
            log.info(i.toString() + " char:  >" + char + "< toString() >" + char.toString() +  "<  replacement:  >" + replaceNonAllowedChars(char.toString(), '*') + "<")
        }
    }
    @Test
    fun moreExamples(){

        val examples = arrayListOf<String>("", " ", "a", "a a", "aaa", "2", "2w", "32", "3 3", ".", "A", "Bn", "..", "žý", "žyž", "ííí", "íxí", "í í", ".c.", "-", "--", "@-@", "---", "_", "-_ ", "   ~", "~~~", " ~ ", "----", "____",
                "ÍÚů", "ščř", "eče", "", "    ")
        val expected = arrayListOf<String>("", " ", "a", "a a", "aaa", "2", "2w", "32", "3 3", ".", "A", "Bn", "..", "xx", "xyx", "xxx", "xxx", "x x", ".c.", "-", "--", "@-@", "---", "_", "-_ ", "   ~", "~~~", " ~ ", "----", "____",
                "xxx", "xxx", "exe", "", "    ")

        for(i in examples.indices){
            val replaced = replaceNonAllowedChars(examples[i],'x')
            assertEquals(replaced, expected[i])
        }
    }
}