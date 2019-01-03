package radim.outfit

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log

import org.junit.Test
import org.junit.runner.RunWith

// import org.junit.Assert.*

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
    @Test
    fun initCB() {
        initCircularBuffer(sharedPreferences.edit())
    }
    @Test
    fun writeAndRead(){
        for(i in 1..23) {
            writeToCircularBuffer("v$i", sharedPreferences)
            printArray(readCircularBuffer(sharedPreferences))
        }
    }
}
