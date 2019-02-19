package radim.outfit.core.export.work

import android.graphics.Color
import android.util.Log
import kotlin.math.roundToInt

// span must not be 0
class ColorCrossFader(fromColor: Int, private val toColor: Int, span: Double){

    // greenColorValue: Int = Color.parseColor("#00ff00")

    private val channExtracts = listOf<(Int)->Int>(
            {it shr 24 and 0xff},//alpha
            {it shr 16 and 0xff},//red
            {it shr 8 and 0xff},//green
            {it and 0xff}//blue
    )
    private var ratios = mutableListOf<Double>()

    init{
        for(i in channExtracts.indices){
            ratios.add((channExtracts[i](fromColor) - channExtracts[i](toColor)) / span)
        }
    }

    // returns color channels in absolute value
    fun colorCrossFade(inSpan: Double): Int{
        val channCrossFades = mutableListOf<Int>()
        for(i in ratios.indices)
            channCrossFades.add(channelCrossFade(ratios[i], inSpan))
        return Color.argb(channExtracts[0](toColor) + channCrossFades[0],
                channExtracts[1](toColor) + channCrossFades[1],
                channExtracts[2](toColor) + channCrossFades[2],
                channExtracts[3](toColor) + channCrossFades[3])
    }
    // inSpan relative toColor toColor
    // inSpan must not overrun bounds of span
    // returns relative toColor toColor value
    private fun channelCrossFade(ratio: Double, inSpan: Double): Int = (ratio * inSpan).roundToInt()
}

class Span(private val from: Double, private val to: Double){
    init{ if(!(to > from)) Log.e("Span", "!to > from") }
    fun getDelta():Double = (to - from)
    fun getInSpanRelativeToTo(inSpanAbsolute: Double): Double = (to - inSpanAbsolute)
    fun isInFrom(value: Double): Boolean = value < from
    fun isInTo(value: Double): Boolean = value > to
}