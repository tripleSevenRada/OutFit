package radim.outfit.core.export.work.locusapiextensions

import locus.api.objects.extra.Location

fun Location.equalsLatLon(other: Location, epsilon: Double): Boolean =
        Math.abs(this.latitude - other.latitude) < epsilon &&
                Math.abs(this.longitude - other.longitude) < epsilon

fun Location.toTrkptRecord(): String = "<trkpt lat=\"${this.latitude}\" lon=\"${this.longitude}\">\n</trkpt>"
fun Location.toWptRecord(): String = "<wpt lat=\"${this.latitude}\" lon=\"${this.longitude}\"></wpt>"