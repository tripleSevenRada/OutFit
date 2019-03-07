package radim.outfit.core.export.work.locusapiextensions

import locus.api.objects.extra.Location

class StarIterator(val baseLoc: Location){
    private var count = 0
    private val step = 0.000134
    private val indicesLat = arrayOf<Double>(0.0,0.6,1.0,1.6,1.0,1.6,1.0,0.6,0.0,-0.6,-1.0,-1.6,-1.0,-1.6,-1.0,-0.6)
    private val indicesLon = arrayOf<Double>(-1.0,-1.6,-1.0,-0.6,0.0,0.6,1.0,1.6,1.0,1.6,1.0,0.6,0.0,-0.6,-1.0,-1.6)

    fun next(): Location?{
        val layer = (count / indicesLat.size) + 1
        val index = count % indicesLat.size
        val additionLat = layer * indicesLat[index] * step
        val additionLon = layer * indicesLon[index] * step
        count++
        if(count > 150) return null else return Location(baseLoc.latitude + additionLat, baseLoc.longitude + additionLon)
    }
}