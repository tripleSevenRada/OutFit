package radim.outfit.core.export.work.locusapiextensions

import com.garmin.fit.CoursePoint
import locus.api.objects.enums.PointRteAction
import locus.api.objects.extra.Point
import radim.outfit.core.export.work.styleNameORIconStyleIconUrlToCoursePoints

const val PARAMETER_STYLE_NAME_FALLBACK = "poi"

fun Point.getWaypointName(): String {
    return if (parameterRteAction != null &&
            parameterRteAction != PointRteAction.PASS_PLACE &&
            parameterRteAction != PointRteAction.UNDEFINED
    ) {
        if (name.isNullOrEmpty()) {
            parameterRteAction.textId ?: PARAMETER_STYLE_NAME_FALLBACK
        } else {
            name
        }
    } else if (
            parameterRteAction != null) {
        if (name.isNullOrEmpty()) {
            if (parameterStyleName.isNullOrEmpty()) PARAMETER_STYLE_NAME_FALLBACK
            else parameterStyleName
        } else name
    } else PARAMETER_STYLE_NAME_FALLBACK
}

/*
String styleName = pt.getParameterStyleName();
if (Validator.isValid(styleName)) {
    return styleName;
}

if (pt.styleNormal != null && pt.styleNormal.getIconStyleIconUrl() != null) {
    return pt.styleNormal.getIconStyleIconUrl();
}
return "";
*/

fun Point.getCoursepointEnumForced(): CoursePoint?{
    val styleName:String = parameterStyleName ?: if (styleNormal != null && styleNormal.iconStyleIconUrl != null)
        styleNormal.iconStyleIconUrl else ""
    var enumForced = styleNameORIconStyleIconUrlToCoursePoints[styleName]
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