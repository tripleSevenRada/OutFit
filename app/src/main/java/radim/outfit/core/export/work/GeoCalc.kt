package radim.outfit.core.export.work

fun Double.toSemiCircles(): Int {
    val d = this * 2147483648.0 / 180.0
    return d.toInt()
}

fun toSemiCirclesJava(d: Double): Int{
    return d.toSemiCircles()
}