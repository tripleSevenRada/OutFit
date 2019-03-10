package radim.outfit

import org.junit.Assert.*
import locus.api.objects.extra.Location
import locus.api.objects.extra.Track
import org.junit.Test
import radim.outfit.core.export.work.locusapiextensions.track_preprocessing.WaypointsRelatedTrackPreprocessing

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
                    WaypointsRelatedTrackPreprocessing(Track(), mutableListOf<String>()).interpolationCoef(locA, locB, locC)
            assertEquals(expected[i], coef, 0.02)

        }
    }

    @Test
    fun `point on a line A B projected by C eg closest to C`(){
        val latsA = listOf<Double>(10.0000001,50.10)
        val lonsA = listOf<Double>(10.0000001,14.10)
        val latsB = listOf<Double>(10.0000005,50.17)
        val lonsB = listOf<Double>(10.0000005,14.32)
        val latsC = listOf<Double>(10.0000006,50.11)
        val lonsC = listOf<Double>(10.0000009,14.25)
        val expectedLans = listOf<Double>(10.00000075, 50.14425891181989)
        val expectedLons = listOf<Double>(10.00000075, 14.239099437148218)

        for(i in latsA.indices){
            val locA = Location()
            locA.latitude = latsA[i]
            locA.longitude = lonsA[i]

            val locB = Location()
            locB.latitude = latsB[i]
            locB.longitude = lonsB[i]

            val locC = Location()
            locC.latitude = latsC[i]
            locC.longitude = lonsC[i]

            val D = WaypointsRelatedTrackPreprocessing(Track(), mutableListOf<String>())
                    .pointOnALineSegmentClosestToPoint(locA,locB,locC)

            assertEquals(expectedLans[i], D.latitude, 0.0000001)
            assertEquals(expectedLons[i], D.longitude, 0.0000001)

        }

    }
}