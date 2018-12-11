package radim.outfit.core.export.work

import com.garmin.fit.CoursePoint
import locus.api.objects.enums.PointRteAction

val tag = "CP"

val routePointActionToCoursePoint: Map<PointRteAction, CoursePoint> = mapOf(
        PointRteAction.LEFT_SLIGHT to CoursePoint.LEFT,
        PointRteAction.LEFT to CoursePoint.LEFT,
        PointRteAction.LEFT_SHARP to CoursePoint.LEFT,
        PointRteAction.RIGHT_SLIGHT to CoursePoint.RIGHT,
        PointRteAction.RIGHT to CoursePoint.RIGHT,
        PointRteAction.RIGHT_SHARP to CoursePoint.RIGHT
)

