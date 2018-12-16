package radim.outfit.core.export.work

import com.garmin.fit.CoursePoint
import locus.api.objects.enums.PointRteAction

const val COURSEPOINTS_LIMIT = 100

val routePointActionsToCoursePoints: Map<PointRteAction, CoursePoint> = mapOf(
        PointRteAction.LEFT_SLIGHT to CoursePoint.SLIGHT_LEFT,
        PointRteAction.RAMP_ON_LEFT to CoursePoint.LEFT_FORK,
        PointRteAction.MERGE_LEFT to CoursePoint.SLIGHT_LEFT,
        PointRteAction.LEFT to CoursePoint.LEFT,
        PointRteAction.LEFT_SHARP to CoursePoint.SHARP_LEFT,
        PointRteAction.U_TURN_LEFT to CoursePoint.SHARP_LEFT,
        PointRteAction.EXIT_LEFT to CoursePoint.LEFT,
        PointRteAction.RIGHT_SLIGHT to CoursePoint.SLIGHT_RIGHT,
        PointRteAction.RAMP_ON_RIGHT to CoursePoint.RIGHT_FORK,
        PointRteAction.MERGE_RIGHT to CoursePoint.SLIGHT_RIGHT,
        PointRteAction.RIGHT to CoursePoint.RIGHT,
        PointRteAction.RIGHT_SHARP to CoursePoint.SHARP_RIGHT,
        PointRteAction.U_TURN_RIGHT to CoursePoint.SHARP_RIGHT,
        PointRteAction.EXIT_RIGHT to CoursePoint.RIGHT,
        PointRteAction.U_TURN to CoursePoint.U_TURN,
        PointRteAction.CONTINUE_STRAIGHT to CoursePoint.STRAIGHT,
        PointRteAction.STAY_STRAIGHT to CoursePoint.STRAIGHT,
        PointRteAction.RAMP_STRAIGHT to CoursePoint.MIDDLE_FORK,
        PointRteAction.MERGE to CoursePoint.STRAIGHT,
        PointRteAction.PASS_PLACE to CoursePoint.GENERIC
)

// low importance low priority
val routePointActionsPrioritized: Map<Int, List<PointRteAction>> = mapOf(
        1 to listOf(PointRteAction.MERGE),
        2 to listOf(PointRteAction.RAMP_STRAIGHT),
        3 to listOf(PointRteAction.STAY_STRAIGHT),
        4 to listOf(PointRteAction.CONTINUE_STRAIGHT),
        5 to listOf(PointRteAction.RAMP_ON_LEFT,PointRteAction.RAMP_ON_RIGHT),
        6 to listOf(PointRteAction.MERGE_LEFT,PointRteAction.MERGE_RIGHT),
        7 to listOf(PointRteAction.LEFT_SLIGHT,PointRteAction.RIGHT_SLIGHT),
        8 to listOf(PointRteAction.EXIT_LEFT,PointRteAction.EXIT_RIGHT),
        9 to listOf(PointRteAction.U_TURN_LEFT,PointRteAction.U_TURN_RIGHT),
        10 to listOf(PointRteAction.U_TURN),
        11 to listOf(PointRteAction.RIGHT_SHARP,PointRteAction.LEFT_SHARP),
        12 to listOf(PointRteAction.RIGHT,PointRteAction.LEFT),
        13 to listOf(PointRteAction.PASS_PLACE)
)

