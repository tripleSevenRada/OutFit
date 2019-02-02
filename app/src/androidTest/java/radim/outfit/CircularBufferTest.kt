package radim.outfit

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import radim.outfit.core.share.work.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class CircularBufferTest {
    // Context of the app under test.
    private val appContext = InstrumentationRegistry.getTargetContext()
    private val sharedPreferences = appContext.getSharedPreferences("radim.outfit.MainActivity",
            Context.MODE_PRIVATE)
    private val buffer = CircularBufferWithSharedPrefs(11)
    @Test
    fun initCB() {
        buffer.initCircularBuffer(sharedPreferences.edit())
    }

    @Test
    fun writeAndRead() {
        for (i in 1..23) {
            buffer.writeToCircularBuffer("v$i", sharedPreferences)
            printArray(buffer.readCircularBuffer(sharedPreferences))
        }
    }

    @Test
    fun testRidEmpty1() {
        val array = arrayOf("a", "", "")
        val expected = arrayOf("a")
        val actual = array.ridEmpty()
        assertEquals(actual, expected)
    }

    @Test
    fun testRidEmpty2() {
        val array = arrayOf("", "a", "")
        val expected = arrayOf("a")
        val actual = array.ridEmpty()
        assertEquals(actual, expected)
    }

    @Test
    fun testRidEmpty3() {
        val array = arrayOf("", "", "a")
        val expected = arrayOf("a")
        val actual = array.ridEmpty()
        assertEquals(actual, expected)
    }

    @Test
    fun testRidDuplicities1() {
        val array = arrayOf("a", "a", "a")
        val expected = arrayOf("a")
        val actual = array.ridDuplicities()
        assertEquals(actual, expected)
    }

    @Test
    fun testRidDuplicities2() {
        val array = arrayOf("a", "b", "a")
        val expected = arrayOf("a","b")
        val actual = array.ridDuplicities()
        assertEquals(actual, expected)
    }

    @Test
    fun testRidDuplicities3() {
        val array = arrayOf("a", "b", "c")
        val expected = arrayOf("a","b","c")
        val actual = array.ridDuplicities()
        assertEquals(actual, expected)
    }

    @Test
    fun testRidDuplicities4() {
        val array = arrayOf("", "", "")
        val expected = arrayOf("")
        val actual = array.ridDuplicities()
        assertEquals(actual, expected)
    }
}
