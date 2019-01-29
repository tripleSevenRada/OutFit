package radim.outfit

import android.graphics.Color
import android.util.Log
import org.junit.Test
import radim.outfit.core.export.work.ColorCrossFader

class ColorCrossFaderTest{

    @Test
    fun testColorCrossFadeEyeBall(){
        val colorFrom:Int = Color.parseColor("#ffffffff")
        Log.i("CF", "colorFrom $colorFrom")
        val colorTo:Int = Color.parseColor("#00000000")
        Log.i("CF", "colorTo $colorTo")
        val CF = ColorCrossFader(colorFrom, colorTo, 25.0)
        for(i in 0..25){
            val colorInt = CF.colorCrossFade(i.toDouble())
            println("__________________________ $i")
            println( "A: ${colorInt.getA()}")
            println( "R: ${colorInt.getR()}")
            println( "G: ${colorInt.getG()}")
            println( "B: ${colorInt.getB()}")
        }
    }
    private fun Int.getA():Int = this shr 24 and 0xff // or color >>> 24
    private fun Int.getR():Int = this shr 16 and 0xff
    private fun Int.getG():Int = this shr 8 and 0xff
    private fun Int.getB():Int = this and 0xff
}