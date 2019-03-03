package radim.outfit.core.export.work

fun Double.toSemiCircles(): Int {
    val d = this * 2147483648.0 / 180.0
    return d.toInt()
}

fun toSemiCirclesJava(d: Double): Int{
    return d.toSemiCircles()
}

/*
Here is a short explanation by Hal Mueller:
    The mapping Garmin uses (180 degrees to 2^31 semicircles) allows them to use a standard 32 bit
    unsigned integer to represent the full 360 degrees of longitude. Thus you get the maximum
    precision that 32 bits allows you (about double what you get from a floating point value),
    and they still get to use integer arithmetic instead of floating point.
 */