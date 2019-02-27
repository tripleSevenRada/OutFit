package radim.outfit

import org.junit.Assert.*
import locus.api.objects.extra.Location
import locus.api.objects.extra.Point
import locus.api.objects.extra.Track
import org.junit.Test
import radim.outfit.core.export.work.locusapiextensions.WaypointsRelatedTrackPreprocessing

class PreprocessingTests{
    @Test
    fun interpolationCoefTest(){

        val locsAlat = listOf<Double>(50.0, 50.0)
        val locsAlon = listOf<Double>(14.0, 14.0)
        val locsBlat = listOf<Double>(50.0, 50.0)
        val locsBlon = listOf<Double>(15.0, 15.0)
        val locsClat = listOf<Double>(50.0, 50.0)
        val locsClon = listOf<Double>(14.5, 14.1)
        val expected = listOf<Double>(0.5, 0.1)

        for(i in 0 until locsAlat.size) {
            val locA = Location()
            locA.latitude = locsAlat[i]
            locA.longitude = locsAlon[i]

            val locB = Location()
            locB.latitude = locsBlat[i]
            locB.longitude = locsBlon[i]

            val locC = Location()
            locC.latitude = locsClat[i]
            locC.longitude = locsClon[i]

            val coef =
                    WaypointsRelatedTrackPreprocessing(Track()).interpolationCoef(locA, locB, locC)
            assertEquals(expected[i], coef, 0.02)

        }
    }
}