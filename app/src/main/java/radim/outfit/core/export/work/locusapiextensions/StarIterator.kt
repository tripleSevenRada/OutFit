package radim.outfit.core.export.work.locusapiextensions

import android.util.Log
import locus.api.objects.extra.Location

class StarIterator(baseLocInit: Location){

    private val tag = "StarIterator"
    private val expectedStepInM = 80.0
    private var baseLoc: Location

    init {
        Log.i(tag,"init")
        baseLoc = baseLocInit
        setUpSteps()
    }

    private var count = 0
    private var stepLat = 0.0008
    private var stepLon = 0.0008
    private val indicesLat = arrayOf(0.0,0.5,1.0,1.5,1.0,1.5,1.0,0.5,0.0,-0.5,-1.0,-1.5,-1.0,-1.5,-1.0,-0.5)
    private val indicesLon = arrayOf(-1.0,-1.5,-1.0,-0.5,0.0,0.5,1.0,1.5,1.0,1.5,1.0,0.5,0.0,-0.5,-1.0,-1.5)

    fun next(): Location?{
        val layer = (count / indicesLat.size) + 1
        val index = count % indicesLat.size
        val additionLat = layer * indicesLat[index] * stepLat
        val additionLon = layer * indicesLon[index] * stepLon
        count++
        return if(count > 80) null else Location(baseLoc.latitude + additionLat, baseLoc.longitude + additionLon)
    }

    fun reset(newLocation: Location){
        baseLoc = newLocation
        count = 0
    }

    private fun setUpSteps(){
        val stepIncrement = 0.00005
        val baseLat = baseLoc.latitude
        val baseLon = baseLoc.longitude
        var latDone = false
        var lonDone = false
        for (i in 1..60){
            val angleIncremented = stepIncrement * i
            if(!latDone) {
                val locMovedLat = Location(baseLat + angleIncremented, baseLon)
                if (locMovedLat.distanceTo(baseLoc).toDouble() > expectedStepInM){
                    latDone = true
                    stepLat = angleIncremented
                    Log.i(tag,"stepLat set: $angleIncremented")
                }
            }
            if(!lonDone) {
                val locMovedLon = Location(baseLat, baseLon + angleIncremented)
                if (locMovedLon.distanceTo(baseLoc).toDouble() > expectedStepInM){
                    lonDone = true
                    stepLon = angleIncremented
                    Log.i(tag,"stepLon set: $angleIncremented")
                }
            }
            if(latDone && lonDone) break
        }
    }
}