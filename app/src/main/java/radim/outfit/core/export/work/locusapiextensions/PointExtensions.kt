package radim.outfit.core.export.work.locusapiextensions

import com.garmin.fit.CoursePoint
import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Point
import radim.outfit.core.export.work.parameterStyleNameToCoursePoints

fun Point.getWaypointName(): String {
    return if (parameterRteAction != null &&
            parameterRteAction != PointRteAction.PASS_PLACE &&
            parameterRteAction != PointRteAction.UNDEFINED
    ) {
        if (name.isNullOrEmpty()) {
            parameterRteAction.textId ?: "poi"
        } else {
            name
        }
    } else if (
            parameterRteAction != null) {
        if (name.isNullOrEmpty()) {
            if (parameterStyleName.isNullOrEmpty()) "poi"
            else parameterStyleName
        } else name
    } else "poi"
}

fun Point.getCoursepointEnumForced(): CoursePoint?{
    val styleName = parameterStyleName ?: ""
    var enumForced = parameterStyleNameToCoursePoints[styleName]
    if (enumForced == null &&
            (parameterRteAction == PointRteAction.PASS_PLACE ||
            parameterRteAction == PointRteAction.UNDEFINED))
        enumForced = CoursePoint.GENERIC
    return enumForced
}

data class WaypointSimplified(val rteIndex: Int,
                              val name: String,
                              val rteAction: PointRteAction,
                              val coursepointEnumForced: CoursePoint?) : Comparable<WaypointSimplified> {
    constructor(point: Point) : this(point.paramRteIndex, point.getWaypointName(),
            point.parameterRteAction, point.getCoursepointEnumForced())

    override fun compareTo(other: WaypointSimplified): Int = this.rteIndex.compareTo(other.rteIndex)
}