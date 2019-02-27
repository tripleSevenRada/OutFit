package radim.outfit.core.export.work

import com.garmin.fit.Sport
import radim.outfit.R

val activityTypesToStringResourceId: Map<Int, Int> = mapOf(
        19 to R.string.activity_type_walking,
        25 to R.string.activity_type_nordic_walking,
        15 to R.string.activity_type_hiking,
        18 to R.string.activity_type_running,
        17 to R.string.activity_type_inline_skating,
        20 to R.string.activity_type_road_cycling,
        22 to R.string.activity_type_mtb,
        31 to R.string.activity_type_scooter,

        1 to R.string.activity_type_cx_skiing,
        2 to R.string.activity_type_downhill_skiing,
        9 to R.string.activity_type_ski_mountaineering,
        3 to R.string.activity_type_snowshoeing,

        26 to R.string.activity_type_sailing,
        7 to R.string.activity_type_canoeing,
        6 to R.string.activity_type_kayaking,
        5 to R.string.activity_type_rowing,
        4 to R.string.activity_type_swimming,

        24 to R.string.activity_type_horse,
        28 to R.string.activity_type_boat,

        12 to R.string.activity_type_car,
        29 to R.string.activity_type_jeep,
        13 to R.string.activity_type_motorcycle,
        14 to R.string.activity_type_truck,
        16 to R.string.activity_type_public_transport,

        0 to R.string.activity_type_generic
)
val activityTypesToGarminSport: Map<Int, com.garmin.fit.Sport> = mapOf(
        19 to Sport.WALKING,
        15 to Sport.HIKING,
        18 to Sport.RUNNING,
        17 to Sport.INLINE_SKATING,
        20 to Sport.CYCLING,
        22 to Sport.CYCLING,
        1 to Sport.CROSS_COUNTRY_SKIING,
        2 to Sport.ALPINE_SKIING,
        3 to Sport.SNOWSHOEING,
        26 to Sport.SAILING,
        5 to Sport.ROWING,
        4 to Sport.SWIMMING,
        24 to Sport.HORSEBACK_RIDING,
        28 to Sport.BOATING,
        12 to Sport.DRIVING,
        29 to Sport.DRIVING,
        13 to Sport.MOTORCYCLING,
        0 to Sport.GENERIC
)