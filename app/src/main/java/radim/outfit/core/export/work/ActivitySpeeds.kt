package radim.outfit.core.export.work

/*
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
*/

// in kmh, converted to m per s in SpeedPickerFragment
val activityTypesToPairOfSpeeds = mapOf<Int, Pair<Int, Int>>(
        0 to Pair(SPEED_MIN_UNIT_AGNOSTIC, SPEED_MAX_UNIT_AGNOSTIC),
        16 to Pair(SPEED_MIN_UNIT_AGNOSTIC, SPEED_MAX_UNIT_AGNOSTIC),
        14 to Pair(40, 110),
        13 to Pair(SPEED_MIN_UNIT_AGNOSTIC, SPEED_MAX_UNIT_AGNOSTIC),
        29 to Pair(5, 80),
        12 to Pair(SPEED_MIN_UNIT_AGNOSTIC, SPEED_MAX_UNIT_AGNOSTIC),
        28 to Pair(5, 100),
        24 to Pair(4,40),
        4 to Pair(2,8),
        5 to Pair(5,20),
        6 to Pair(5,20),
        7 to Pair(5,20),
        26 to Pair(4, 40),
        3 to Pair(2, 10),
        9 to Pair(2, 20),
        2 to Pair(10, 120),
        1 to Pair(5, 22),
        31 to Pair(4, 22),
        22 to Pair(5, 34),
        20 to Pair(14, 36),
        17 to Pair(4, 22),
        18 to Pair(6, 20),
        15 to Pair(3, 8),
        25 to Pair(3, 8),
        19 to Pair(3, 8)
)