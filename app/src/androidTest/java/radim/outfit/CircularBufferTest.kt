package radim.outfit

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import junit.framework.Assert

import org.junit.Test
import org.junit.runner.RunWith

import radim.outfit.core.share.work.*
import java.util.*

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
    private val buffer = CircularBufferWithSharedPrefs(10)
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
        Assert.assertTrue(actual.contentEquals(expected))
    }

    @Test
    fun testRidEmpty2() {
        val array = arrayOf("", "a", "")
        val expected = arrayOf("a")
        val actual = array.ridEmpty()
        Assert.assertTrue(actual.contentEquals(expected))
    }

    @Test
    fun testRidEmpty3() {
        val array = arrayOf("", "", "a")
        val expected = arrayOf("a")
        val actual = array.ridEmpty()
        Assert.assertTrue(actual.contentEquals(expected))
    }

    @Test
    fun testRidDuplicities1() {
        val array = arrayOf("a", "a", "a")
        val expected = arrayOf("a")
        val actual = array.ridDuplicities()
        Assert.assertTrue(actual.contentEquals(expected))
    }

    @Test
    fun testRidDuplicities2() {
        val array = arrayOf("a", "b", "a")
        val expected = arrayOf("a", "b")
        val actual = array.ridDuplicities()
        Assert.assertTrue(actual.contentEquals(expected))
    }

    @Test
    fun testRidDuplicities3() {
        val array = arrayOf("a", "b", "c")
        val expected = arrayOf("a", "b", "c")
        val actual = array.ridDuplicities()
        Assert.assertTrue(actual.contentEquals(expected))
    }

    @Test
    fun testRidDuplicities4() {
        val array = arrayOf("", "", "")
        val expected = arrayOf("")
        val actual = array.ridDuplicities()
        Assert.assertTrue(actual.contentEquals(expected))
    }

    @Test
    fun bothContains() {
        val circ1 = CircularBufferWithSharedPrefs(11)
        val circ2 = CircularBufferWithSharedPrefs(12)
        circ1.initCircularBuffer(sharedPreferences.edit())
        circ2.initCircularBuffer(sharedPreferences.edit())
        repeat(12) {
            repeat(77) {
                val randString = getRandomString(12)
                repeat(Random().nextInt(11)) {
                    circ1.writeToCircularBuffer(randString, sharedPreferences)
                    circ2.writeToCircularBuffer(randString, sharedPreferences)
                }
                val read1 = circ1.readCircularBuffer(sharedPreferences)
                val read2 = circ2.readCircularBuffer(sharedPreferences)
                Assert.assertTrue(read1.contentEquals(read2))
            }
            val read11 = circ1.readCircularBuffer(sharedPreferences)
            val read22 = circ2.readCircularBuffer(sharedPreferences)
            val short = read11.ridDuplicities().ridEmpty()
            Assert.assertTrue(short.size > 0)
            printArray(short)
            short.forEach {
                Assert.assertTrue(read22.contains(it))
            }
        }
    }
}
