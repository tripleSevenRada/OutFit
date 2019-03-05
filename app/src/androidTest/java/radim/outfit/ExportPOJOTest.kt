package radim.outfit

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import locus.api.objects.extra.Track

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import radim.outfit.core.export.logic.ExportPOJO
import radim.outfit.core.export.logic.mergeExportPOJOS
import radim.outfit.core.export.work.locusapiextensions.TrackContainer
import java.io.File

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExportPOJOTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("radim.outfit", appContext.packageName)

        val pojo1 = ExportPOJO(File("/"),"filename.fit", TrackContainer(Track(), mutableMapOf()) )
        val pojo2 = ExportPOJO(null, null, null)

        val pojo3 = mergeExportPOJOS(pojo1, pojo2)
        assertEquals(pojo1, pojo3)

        val pojo4 = ExportPOJO(File("/doc"),"filename.fit", TrackContainer(Track(), mutableMapOf()) )
        val pojo5 = mergeExportPOJOS(pojo1, pojo4)
        assertEquals(pojo4, pojo5)
    }
}
