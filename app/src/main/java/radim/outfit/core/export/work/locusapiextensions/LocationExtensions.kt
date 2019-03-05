package radim.outfit.core.export.work.locusapiextensions

import locus.api.objects.extra.Location

fun Location.equalsLatLon(other: Location, epsilon: Double): Boolean =
        Math.abs(this.latitude - other.latitude) < epsilon &&
                Math.abs(this.longitude - other.longitude) < epsilon