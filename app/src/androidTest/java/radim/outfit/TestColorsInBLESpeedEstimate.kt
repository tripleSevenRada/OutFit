package radim.outfit

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.support.v7.app.AppCompatActivity
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import radim.outfit.core.share.logic.getSpannableDownloadInfo

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class TestColorsInBLESpeedEstimate {
    @Test
    fun iterateSecondsTotal() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        //Assert.assertEquals("radim.outfit", appContext.packageName)
        for (i in 0..300) {
            val spannableDownloadInfo = getSpannableDownloadInfo(
                    i,
                    appContext,
                    1024
            )
        }
    }
}